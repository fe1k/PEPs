-- ============================================================
-- 20_seed_archive_test.sql
-- Données de test pour l'archivage (Audit_Log + Interaction).
-- Remplace 100_archive_audit.sql et 101_archive_test.sql.
--
-- Les dates sont RELATIVES à NOW() pour que le jeu de test reste
-- valable quel que soit le jour où la base est créée :
--   - archivables     : entre 4 et 12 mois dans le passé
--   - non archivables : moins de 3 mois (pour vérifier qu'elles restent)
--
-- Dépend de 10_seed_data.sql (users, species, modules, sounds).
-- ⚠️ Non idempotent : relancer le script duplique les lignes.
-- ============================================================

-- ------------------------------------------------------------
-- Audit_Log
-- (old_value / new_value n'existent plus → résumés dans details)
-- ------------------------------------------------------------
INSERT INTO peps.Audit_Log (action, entity_type, entity_id, logged_at, details, id_user)
SELECT v.action, v.entity_type, v.entity_id, NOW() - v.age, v.details, u.id_user
FROM (VALUES
    -- Archivables
    ('CREATE', 'module', 1,  INTERVAL '4 months',  'Création du module "Module Bassin Principal" (Dauphin)', 'admin'),
    ('UPDATE', 'sound',  2,  INTERVAL '5 months',  'Sound "Clics de Dauphin" : type Ambiance → Vocal',        'dauphin'),
    ('DELETE', 'module', 99, INTERVAL '6 months',  'Suppression du module obsolète "Module Test"',           'admin'),
    ('CREATE', 'sound',  10, INTERVAL '7 months',  'Ajout du son "Forêt Tropicale" (Ara)',                   'aras'),
    ('UPDATE', 'module', 5,  INTERVAL '8 months',  'Module "Module Perchoir Ara" : mode Manuel → Automatique','admin'),
    ('CREATE', 'user',   3,  INTERVAL '9 months',  'Création du compte "aras" (role editor)',                'admin'),
    ('UPDATE', 'user',   2,  INTERVAL '10 months', 'Compte "dauphin" : role viewer → editor',                'admin'),
    ('DELETE', 'sound',  98, INTERVAL '11 months', 'Suppression du son vocal obsolète "Son Ancien"',         'dauphin'),
    ('CREATE', 'module', 7,  INTERVAL '12 months', 'Création du module "GESTION ARAS"',                      'admin'),
    -- Non archivables (récents)
    ('UPDATE', 'module', 1,  INTERVAL '10 days',   'Module "Module Bassin Principal" : volume 80 → 90',      'dauphin'),
    ('UPDATE', 'module', 4,  INTERVAL '1 month',   'Module "Module Volière Ara" : volume 70 → 75',           'aras')
) AS v(action, entity_type, entity_id, age, details, login)
LEFT JOIN peps.users u ON u.login = v.login;

-- ------------------------------------------------------------
-- Interaction
-- Module et son choisis par espèce (pas d'id codé en dur)
-- ------------------------------------------------------------
WITH target AS (
    SELECT s.species_name,
           (SELECT MIN(m.id_module) FROM peps.IoT_Module m
             WHERE m.id_species = s.id_species AND m.is_active) AS id_module,
           (SELECT ps.id_sound FROM peps.Playlist_Sounds ps
             JOIN peps.IoT_Module m ON m.id_playlist = ps.id_playlist
            WHERE m.id_species = s.id_species
            ORDER BY ps.play_order LIMIT 1) AS id_sound
    FROM peps.Species s
)
INSERT INTO peps.Interaction (interaction_type, triggered_at, id_module, id_sound)
SELECT v.interaction_type, NOW() - v.age, t.id_module, t.id_sound
FROM (VALUES
    -- Dauphin : archivables
    ('Dauphin', 'Head',  INTERVAL '4 months'),
    ('Dauphin', 'Tail',  INTERVAL '5 months'),
    ('Dauphin', 'Head',  INTERVAL '6 months'),
    ('Dauphin', 'Tail',  INTERVAL '7 months'),
    ('Dauphin', 'Head',  INTERVAL '8 months'),
    ('Dauphin', 'Tail',  INTERVAL '9 months'),
    ('Dauphin', 'Head',  INTERVAL '10 months'),
    ('Dauphin', 'Tail',  INTERVAL '11 months'),
    ('Dauphin', 'Head',  INTERVAL '12 months'),
    -- Ara : archivables
    ('Ara',     'Bec',   INTERVAL '4 months'),
    ('Ara',     'Patte', INTERVAL '5 months'),
    ('Ara',     'Bec',   INTERVAL '6 months'),
    ('Ara',     'Patte', INTERVAL '7 months'),
    ('Ara',     'Bec',   INTERVAL '8 months'),
    ('Ara',     'Patte', INTERVAL '9 months'),
    ('Ara',     'Bec',   INTERVAL '10 months'),
    ('Ara',     'Patte', INTERVAL '11 months'),
    ('Ara',     'Bec',   INTERVAL '12 months'),
    -- Non archivables (récentes)
    ('Dauphin', 'Head',  INTERVAL '2 weeks'),
    ('Ara',     'Bec',   INTERVAL '1 month')
) AS v(species_name, interaction_type, age)
JOIN target t ON t.species_name = v.species_name;

-- ------------------------------------------------------------
-- Vérification : archivable / non archivable par table
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