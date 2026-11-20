-- ============================================================
-- 10_seed_data.sql
-- Seed / demo data, compatible with 01_create_tables.sql
--
-- Demo users (password: "admin" for all)
--   admin   → ROLE_ADMIN    (Macaws, Saimiris, Dolphins)
--   dauphin → ROLE_SOIGNEUR (Dolphins)
--   aras    → ROLE_SOIGNEUR (Macaws)
-- Verified BCrypt hash (strength 10) for "admin".
-- Demo passwords only — must be changed outside dev environments.
--
-- Idempotent script: can be re-run safely without errors.
-- ============================================================

-- ------------------------------------------------------------
-- Species
-- ------------------------------------------------------------
INSERT INTO peps.Species (id_species, name, description) VALUES
    (1, 'Macaws',   'Aras (Ara ararauna, Ara macao…)'),
    (2, 'Saimiris', 'Saïmiris (Saimiri sciureus)'),
    (3, 'Dolphins', 'Grands dauphins (Tursiops truncatus)')
ON CONFLICT (id_species) DO NOTHING;

-- ------------------------------------------------------------
-- Users
-- ------------------------------------------------------------
INSERT INTO peps.users (login, password_hash, role, is_active) VALUES
    ('admin',   '$2a$10$addEtMm/UEHiwBiVDQ9N8OT0OLfsGAwhnu03mUz.oJ1wMvlGhlS5C', 'ROLE_ADMIN',    true),
    ('dauphin', '$2a$10$addEtMm/UEHiwBiVDQ9N8OT0OLfsGAwhnu03mUz.oJ1wMvlGhlS5C', 'ROLE_SOIGNEUR', true),
    ('aras',    '$2a$10$addEtMm/UEHiwBiVDQ9N8OT0OLfsGAwhnu03mUz.oJ1wMvlGhlS5C', 'ROLE_SOIGNEUR', true)
ON CONFLICT (login) DO NOTHING;

-- ------------------------------------------------------------
-- User_Species (resolved by login rather than hardcoded IDs)
-- ------------------------------------------------------------
INSERT INTO peps.User_Species (id_user, id_species)
SELECT u.id_user, s.id_species
FROM (VALUES ('admin',   'Macaws'),
             ('admin',   'Saimiris'),
             ('admin',   'Dolphins'),
             ('dauphin', 'Dolphins'),
             ('aras',    'Macaws')) AS v(login, name)
JOIN peps.users   u ON u.login = v.login
JOIN peps.Species s ON s.name = v.name
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
-- Playlists + Playlist_Sound
-- ------------------------------------------------------------
INSERT INTO peps.Playlist (id_playlist, name, playback_mode) VALUES
    (1, 'Playlist Dauphins', 'SEQUENTIAL'),
    (2, 'Playlist Aras',     'RANDOM')
ON CONFLICT (id_playlist) DO NOTHING;

INSERT INTO peps.Playlist_Sound (id_playlist, id_sound, play_order) VALUES
    (1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4), (1, 5, 5),
    (2, 6, 1), (2, 7, 2), (2, 8, 3), (2, 9, 4), (2, 10, 5)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- IoT_Module
-- ------------------------------------------------------------
INSERT INTO peps.IoT_Module (id_module, name, ip_address, status, battery_level, volume, current_mode, is_active, last_seen_at, id_species, id_playlist) VALUES
    (1, 'Module Bassin Principal',     '192.168.2.10', 'ONLINE',  87,   90, 'AUTOMATIC',   true,  '2026-01-21 15:23:40', 3, 1),
    (2, 'Module Zone Repos',           '192.168.2.11', 'ONLINE',  64,   50, 'MANUAL',      true,  '2026-01-21 15:13:40', 3, 1),
    (3, 'Module Piscine Entrainement', '192.168.2.12', 'ONLINE',  92,   70, 'AUTOMATIC',   true,  '2026-01-21 15:27:40', 3, 1),
    (4, 'Module Volière Ara',          '192.168.3.10', 'ONLINE',  78,   75, 'AUTOMATIC',   true,  '2026-01-30 11:10:19', 1, 2),
    (5, 'Module Perchoir Ara',         '192.168.3.11', 'ONLINE',  45,   60, 'MANUAL',      true,  '2026-01-21 15:53:44', 1, 2),
    (6, 'Module Cage Principale',      '192.168.3.12', 'ONLINE',  81,   85, 'AUTOMATIC',   true,  '2026-01-21 15:23:40', 1, 2),
    (7, 'GESTION ARAS',                '192.168.1.1',  'OFFLINE', NULL, 50, 'MANUAL',      false, '2026-01-21 15:36:34', 1, NULL)
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
-- Séquences: reset to the actual MAX(id)
-- (otherwise the next INSERT without an explicit ID will conflict with the hardcoded IDs above)
-- ------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('peps.species',     'id_species'),     (SELECT MAX(id_species)     FROM peps.species));
SELECT setval(pg_get_serial_sequence('peps.users',       'id_user'),        (SELECT MAX(id_user)        FROM peps.users));
SELECT setval(pg_get_serial_sequence('peps.sound',       'id_sound'),       (SELECT MAX(id_sound)       FROM peps.sound));
SELECT setval(pg_get_serial_sequence('peps.playlist',    'id_playlist'),    (SELECT MAX(id_playlist)    FROM peps.playlist));
SELECT setval(pg_get_serial_sequence('peps.iot_module',  'id_module'),      (SELECT MAX(id_module)      FROM peps.iot_module));
SELECT setval(pg_get_serial_sequence('peps.interaction', 'id_interaction'), (SELECT MAX(id_interaction) FROM peps.interaction));
