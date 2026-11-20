-- ============================================================
-- 20_seed_archive_test.sql
-- Test data for archiving (Audit_Log + Interaction).
-- Replaces 100_archive_audit.sql and 101_archive_test.sql.
--
-- Dates are RELATIVE to NOW() so that the test dataset remains
-- valid regardless of the day the database is created:
--   - archivable     : between 4 and 12 months in the past
--   - non-archivable : less than 3 months old (to verify they remain)
--
-- Depends on 10_seed_data.sql (users, species, modules, sounds).
-- Not idempotent: re-running the script will duplicate rows.
-- ============================================================

-- ------------------------------------------------------------
-- Audit_Log
-- (old_value / new_value no longer exist → summarized in details)
-- ------------------------------------------------------------
INSERT INTO peps.Audit_Log (action, entity_type, entity_id, logged_at, details, id_user)
SELECT v.action, v.entity_type, v.entity_id, NOW() - v.age, v.details, u.id_user
FROM (VALUES
    -- Archivable
    ('CREATE', 'module', 1,  INTERVAL '4 months',  'Création du module "Module Bassin Principal" (Dolphins)', 'admin'),
    ('UPDATE', 'sound',  2,  INTERVAL '5 months',  'Sound "Clics de Dauphin" : type Ambiance → Vocal',        'dauphin'),
    ('DELETE', 'module', 99, INTERVAL '6 months',  'Suppression du module obsolète "Module Test"',           'admin'),
    ('CREATE', 'sound',  10, INTERVAL '7 months',  'Ajout du son "Forêt Tropicale" (Macaws)',                   'aras'),
    ('UPDATE', 'module', 5,  INTERVAL '8 months',  'Module "Module Perchoir Ara" : mode MANUAL → AUTOMATIC','admin'),
    ('CREATE', 'user',   3,  INTERVAL '9 months',  'Création du compte "aras" (ROLE_SOIGNEUR)',                'admin'),
    ('UPDATE', 'user',   2,  INTERVAL '10 months', 'Compte "dauphin" : role ROLE_CHERCHEUR → ROLE_SOIGNEUR',                'admin'),
    ('DELETE', 'sound',  98, INTERVAL '11 months', 'Suppression du son vocal obsolète "Son Ancien"',         'dauphin'),
    ('CREATE', 'module', 7,  INTERVAL '12 months', 'Création du module "GESTION ARAS"',                      'admin'),
    -- Non-archivable (recent)
    ('UPDATE', 'module', 1,  INTERVAL '10 days',   'Module "Module Bassin Principal" : volume 80 → 90',      'dauphin'),
    ('UPDATE', 'module', 4,  INTERVAL '1 month',   'Module "Module Volière Ara" : volume 70 → 75',           'aras')
) AS v(action, entity_type, entity_id, age, details, login)
LEFT JOIN peps.users u ON u.login = v.login;

-- ------------------------------------------------------------
-- Interaction
-- Module and sound selected by species (no hardcoded IDs)
-- ------------------------------------------------------------
WITH target AS (
    SELECT s.name,
           (SELECT MIN(m.id_module) FROM peps.IoT_Module m
             WHERE m.id_species = s.id_species AND m.is_active) AS id_module,
           (SELECT ps.id_sound FROM peps.Playlist_Sound ps
             JOIN peps.IoT_Module m ON m.id_playlist = ps.id_playlist
            WHERE m.id_species = s.id_species
            ORDER BY ps.play_order LIMIT 1) AS id_sound
    FROM peps.Species s
)
INSERT INTO peps.Interaction (interaction_type, triggered_at, id_module, id_sound)
SELECT v.interaction_type, NOW() - v.age, t.id_module, t.id_sound
FROM (VALUES
    -- Dolphins: archivable
    ('Dolphins', 'Head',  INTERVAL '4 months'),
    ('Dolphins', 'Tail',  INTERVAL '5 months'),
    ('Dolphins', 'Head',  INTERVAL '6 months'),
    ('Dolphins', 'Tail',  INTERVAL '7 months'),
    ('Dolphins', 'Head',  INTERVAL '8 months'),
    ('Dolphins', 'Tail',  INTERVAL '9 months'),
    ('Dolphins', 'Head',  INTERVAL '10 months'),
    ('Dolphins', 'Tail',  INTERVAL '11 months'),
    ('Dolphins', 'Head',  INTERVAL '12 months'),
    -- Macaws: archivable
    ('Macaws',   'Bec',   INTERVAL '4 months'),
    ('Macaws',   'Patte', INTERVAL '5 months'),
    ('Macaws',   'Bec',   INTERVAL '6 months'),
    ('Macaws',   'Patte', INTERVAL '7 months'),
    ('Macaws',   'Bec',   INTERVAL '8 months'),
    ('Macaws',   'Patte', INTERVAL '9 months'),
    ('Macaws',   'Bec',   INTERVAL '10 months'),
    ('Macaws',   'Patte', INTERVAL '11 months'),
    ('Macaws',   'Bec',   INTERVAL '12 months'),
    -- Non-archivable (recent)
    ('Dolphins', 'Head',  INTERVAL '2 weeks'),
    ('Macaws',   'Bec',   INTERVAL '1 month')
) AS v(name, interaction_type, age)
JOIN target t ON t.name = v.name;

-- ------------------------------------------------------------
-- Verification: archivable / non-archivable per table
-- ------------------------------------------------------------
SELECT 'audit_log' AS table_name,
       COUNT(*) FILTER (WHERE logged_at <  NOW() - INTERVAL '3 months') AS archivables,
       COUNT(*) FILTER (WHERE logged_at >= NOW() - INTERVAL '3 months') AS recentes
FROM peps.Audit_Log
UNION ALL
SELECT 'interaction',
       COUNT(*) FILTER (WHERE triggered_at <  NOW() - INTERVAL '3 months'),
       COUNT(*) FILTER (WHERE triggered_at >= NOW() - INTERVAL '3 months')
FROM peps.Interaction;
