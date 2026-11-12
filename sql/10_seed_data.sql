-- ============================================================
-- 10_seed_data.sql
-- Données de base / démo, compatibles avec 01_create_tables.sql
--
-- Utilisateurs de démo (mot de passe : "admin" pour tous)
--   admin   → role admin   (accès à toutes les espèces)
--   dauphin → role editor  (espèce Dauphin)
--   aras    → role editor  (espèce Ara)
-- Hash BCrypt (strength 10) vérifié pour "admin".
-- Mots de passe de démo uniquement — à changer hors dev.
--
-- Script idempotent : peut être relancé sans erreur.
-- ============================================================

-- ------------------------------------------------------------
-- Species
-- ------------------------------------------------------------
INSERT INTO peps.Species (id_species, species_name, description) VALUES
    (1, 'Dauphin', 'Grand dauphin (Tursiops truncatus)'),
    (2, 'Ara',     'Ara bleu (Ara ararauna)')
ON CONFLICT (id_species) DO NOTHING;

-- ------------------------------------------------------------
-- Users
-- ------------------------------------------------------------
INSERT INTO peps.users (login, password_hash, role, is_active) VALUES
    ('admin',   '$2a$10$addEtMm/UEHiwBiVDQ9N8OT0OLfsGAwhnu03mUz.oJ1wMvlGhlS5C', 'admin',  true),
    ('dauphin', '$2a$10$addEtMm/UEHiwBiVDQ9N8OT0OLfsGAwhnu03mUz.oJ1wMvlGhlS5C', 'editor', true),
    ('aras',    '$2a$10$addEtMm/UEHiwBiVDQ9N8OT0OLfsGAwhnu03mUz.oJ1wMvlGhlS5C', 'editor', true)
ON CONFLICT (login) DO NOTHING;

-- ------------------------------------------------------------
-- User_Species (on passe par le login, pas par un id fixe)
-- ------------------------------------------------------------
INSERT INTO peps.User_Species (id_user, id_species)
SELECT u.id_user, s.id_species
FROM (VALUES ('dauphin', 'Dauphin'),
             ('aras',    'Ara')) AS v(login, species_name)
JOIN peps.users   u ON u.login = v.login
JOIN peps.Species s ON s.species_name = v.species_name
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- Sounds
-- ------------------------------------------------------------
INSERT INTO peps.Sound (id_sound, name, sound_type, extension, minio_key, duration_seconds) VALUES
    (1,  'Chant des Baleines', 'Naturelle', 'mp3', NULL, NULL),
    (2,  'Clics de Dauphin',   'Vocal',     'wav', NULL, NULL),
    (3,  'Vagues Océan',       'Ambiance',  'mp3', NULL, NULL),
    (4,  'Sonar Marin',        'Vocal',     'mp3', NULL, NULL),
    (5,  'Musique Aquatique',  'Ambiance',  'wav', NULL, NULL),
    (6,  'Cri Ara Bleu',       'Vocal',     'mp3', NULL, NULL),
    (7,  'Musique Tropicale',  'Ambiance',  'mp3', NULL, NULL),
    (8,  'Pluie Amazonienne',  'Naturelle', 'wav', NULL, NULL),
    (9,  'Chant Perroquet',    'Vocal',     'mp3', NULL, NULL),
    (10, 'Forêt Tropicale',    'Ambiance',  'wav', NULL, NULL)
ON CONFLICT (id_sound) DO NOTHING;

-- ------------------------------------------------------------
-- Playlists + Playlist_Sounds
-- ------------------------------------------------------------
INSERT INTO peps.Playlist (id_playlist, name, playback_mode) VALUES
    (1, 'Playlist Dauphins', 'SEQUENTIAL'),
    (2, 'Playlist Aras',     'RANDOM')
ON CONFLICT (id_playlist) DO NOTHING;

INSERT INTO peps.Playlist_Sounds (id_playlist, id_sound, play_order) VALUES
    (1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4), (1, 5, 5),
    (2, 6, 1), (2, 7, 2), (2, 8, 3), (2, 9, 4), (2, 10, 5)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- IoT_Module
-- ------------------------------------------------------------
INSERT INTO peps.IoT_Module (id_module, name, ip_address, status, battery_level, volume, current_mode, is_active, last_seen_at, id_species, id_playlist) VALUES
    (1, 'Module Bassin Principal',     '192.168.2.10', 'actif',   87,   90, 'Automatique', true,  '2026-01-21 15:23:40', 1, 1),
    (2, 'Module Zone Repos',           '192.168.2.11', 'actif',   64,   50, 'Manuel',      true,  '2026-01-21 15:13:40', 1, 1),
    (3, 'Module Piscine Entrainement', '192.168.2.12', 'actif',   92,   70, 'Automatique', true,  '2026-01-21 15:27:40', 1, 1),
    (4, 'Module Volière Ara',          '192.168.3.10', 'actif',   78,   75, 'Automatique', true,  '2026-01-30 11:10:19', 2, 2),
    (5, 'Module Perchoir Ara',         '192.168.3.11', 'actif',   45,   60, 'Manuel',      true,  '2026-01-21 15:53:44', 2, 2),
    (6, 'Module Cage Principale',      '192.168.3.12', 'actif',   81,   85, 'Automatique', true,  '2026-01-21 15:23:40', 2, 2),
    (7, 'GESTION ARAS',                '192.168.1.1',  'inactif', NULL, 50, 'Manuel',      false, '2026-01-21 15:36:34', 2, NULL)
ON CONFLICT (id_module) DO NOTHING;

-- ------------------------------------------------------------
-- Interaction
-- ------------------------------------------------------------
INSERT INTO peps.Interaction (id_interaction, interaction_type, triggered_at, id_module, id_sound) VALUES
    (1, 'Head',  '2026-01-21 15:18:40', 1, 1),
    (2, 'Tail',  '2026-01-21 15:20:40', 1, 2),
    (3, 'Bec',   '2026-01-21 15:08:40', 4, 6),
    (4, 'Patte', '2026-01-21 15:13:40', 5, 7)
ON CONFLICT (id_interaction) DO NOTHING;

-- ------------------------------------------------------------
-- Séquences : les recaler sur le MAX(id) réel
-- (sinon le prochain INSERT sans id entre en conflit avec les ids fixés ci-dessus)
-- ------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('peps.species',     'id_species'),     (SELECT MAX(id_species)     FROM peps.species));
SELECT setval(pg_get_serial_sequence('peps.users',       'id_user'),        (SELECT MAX(id_user)        FROM peps.users));
SELECT setval(pg_get_serial_sequence('peps.sound',       'id_sound'),       (SELECT MAX(id_sound)       FROM peps.sound));
SELECT setval(pg_get_serial_sequence('peps.playlist',    'id_playlist'),    (SELECT MAX(id_playlist)    FROM peps.playlist));
SELECT setval(pg_get_serial_sequence('peps.iot_module',  'id_module'),      (SELECT MAX(id_module)      FROM peps.iot_module));
SELECT setval(pg_get_serial_sequence('peps.interaction', 'id_interaction'), (SELECT MAX(id_interaction) FROM peps.interaction));
