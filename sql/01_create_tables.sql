-- ============================================================
-- 01_create_tables.sql
-- ============================================================

CREATE TABLE peps.Species (
    id_species   SERIAL PRIMARY KEY,
    name varchar(50) NOT NULL UNIQUE,
    description  TEXT
);

CREATE TABLE peps.Playlist (
    id_playlist   SERIAL PRIMARY KEY,
    name          varchar(255) NOT NULL,
    playback_mode varchar(255) NOT NULL
);

CREATE TABLE peps.Users (
    id_user       SERIAL PRIMARY KEY,
    login         varchar(50)  NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role          varchar(50),
    is_active     boolean   NOT NULL DEFAULT true,
    created_at    timestamp NOT NULL DEFAULT NOW(),
    updated_at    timestamp NOT NULL DEFAULT NOW()
);

CREATE TABLE peps.Sound (
    id_sound         SERIAL PRIMARY KEY,
    name             varchar(255) NOT NULL,
    sound_type       varchar(50)  NOT NULL,
    extension        varchar(10)  NOT NULL,
    minio_key        varchar(255),
    duration_seconds integer,
    created_at       timestamp NOT NULL DEFAULT NOW()
);

CREATE TABLE peps.IoT_Module (
    id_module     SERIAL PRIMARY KEY,
    name          varchar(255) NOT NULL,
    ip_address    varchar(50)  NOT NULL,
    status        varchar(50)  NOT NULL,
    battery_level integer,
    volume        integer      NOT NULL,
    current_mode  varchar(50)  NOT NULL,
    is_active     boolean      NOT NULL DEFAULT true,
    last_seen_at  timestamp,
    id_species    integer NOT NULL,
    id_playlist   integer,

    CONSTRAINT fk_module_species
        FOREIGN KEY (id_species) REFERENCES peps.Species(id_species)
        ON DELETE RESTRICT,
    CONSTRAINT fk_module_playlist
        FOREIGN KEY (id_playlist) REFERENCES peps.Playlist(id_playlist)
        ON DELETE SET NULL
);

CREATE TABLE peps.Interaction (
    id_interaction   SERIAL PRIMARY KEY,
    interaction_type varchar(50) NOT NULL,
    triggered_at     timestamp   NOT NULL DEFAULT NOW(),
    id_module        integer,
    id_sound         integer,

    CONSTRAINT fk_interaction_module
        FOREIGN KEY (id_module) REFERENCES peps.IoT_Module(id_module)
        ON DELETE SET NULL,
    CONSTRAINT fk_interaction_sound
        FOREIGN KEY (id_sound) REFERENCES peps.Sound(id_sound)
        ON DELETE SET NULL
);

CREATE TABLE peps.Playlist_Sound (
    id_playlist_sound SERIAL PRIMARY KEY,
    id_playlist integer NOT NULL,
    id_sound    integer NOT NULL,
    play_order  integer NOT NULL,

    CONSTRAINT uq_playlist_order UNIQUE (id_playlist, play_order),
    CONSTRAINT fk_ps_playlist
        FOREIGN KEY (id_playlist) REFERENCES peps.Playlist(id_playlist)
        ON DELETE CASCADE,
    CONSTRAINT fk_ps_sound
        FOREIGN KEY (id_sound) REFERENCES peps.Sound(id_sound)
        ON DELETE CASCADE
);

CREATE TABLE peps.User_Species (
    id_user    integer NOT NULL,
    id_species integer NOT NULL,

    PRIMARY KEY (id_user, id_species),
    CONSTRAINT fk_us_user
        FOREIGN KEY (id_user) REFERENCES peps.users(id_user)
        ON DELETE CASCADE,
    CONSTRAINT fk_us_species
        FOREIGN KEY (id_species) REFERENCES peps.Species(id_species)
        ON DELETE CASCADE
);

CREATE TABLE peps.Observation_Measurement (
    id_observation   SERIAL PRIMARY KEY,
    recorded_at      timestamp   NOT NULL DEFAULT NOW(),
    data_source      varchar(50) NOT NULL,
    measurement_type varchar(50) NOT NULL,
    numeric_value    double precision,
    text_value       TEXT,
    json_data        jsonb,
    id_species       integer NOT NULL,
    id_user          integer,
    id_module        integer,

    CONSTRAINT fk_obs_species
        FOREIGN KEY (id_species) REFERENCES peps.Species(id_species)
        ON DELETE RESTRICT,
    CONSTRAINT fk_obs_user
        FOREIGN KEY (id_user) REFERENCES peps.users(id_user)
        ON DELETE SET NULL,
    CONSTRAINT fk_obs_module
        FOREIGN KEY (id_module) REFERENCES peps.IoT_Module(id_module)
        ON DELETE SET NULL
);

CREATE TABLE peps.Audit_Log (
    id_log      SERIAL PRIMARY KEY,
    action      varchar(50) NOT NULL,
    entity_type varchar(50) NOT NULL,
    entity_id   integer,
    logged_at   timestamp NOT NULL DEFAULT NOW(),
    details     TEXT,
    id_user     integer,

    CONSTRAINT fk_audit_user
        FOREIGN KEY (id_user) REFERENCES peps.users(id_user)
        ON DELETE SET NULL
);