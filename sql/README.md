# Database — SQL Scripts

This directory contains the SQL scripts that initialize the PostgreSQL database.
Docker mounts it as `/docker-entrypoint-initdb.d/` and runs every `.sql` file **in alphanumeric order**, **only on a fresh/empty volume**.

---

## How It Works (Fresh Install)

```bash
docker compose up          # first run: scripts are executed
```

If the volume already exists, Docker will **not** re-run the scripts. To force a re-initialization (⚠️ deletes all data):

```bash
docker compose down -v
docker compose up
```

---

## Script Execution Order

| File | Purpose | Idempotent |
|------|---------|-----------|
| `00_schema.sql` | Creates the `peps` schema | ✅ |
| `01_create_tables.sql` | Creates all tables, PKs, FKs and constraints (matches the MPD) | ❌ (fresh DB only) |
| `10_seed_data.sql` | Base / demo dataset: species, users, playlists, sounds, modules, interactions | ✅ |
| `20_seed_archive_test.sql` | Test data for the archive feature (old + recent audit logs and interactions) | ❌ |

### Naming convention

- Always use a **2-digit prefix** (`00`–`99`). Docker sorts as text, so `100_x.sql` would run **before** `10_x.sql` and `99_x.sql`.
- Leave gaps between numbers (`00`, `01`, `10`, `20`…) so new scripts can be inserted without renaming.
- Ranges: `0x` = structure, `1x` = base data, `2x` = test data.
- Files without the `.sql` extension (e.g. `.sql.disabled`) are ignored by Docker.

---

## Data Model

```
peps.species                  → Animal species (Dauphin, Ara…)
peps.users                    → Application users (login, password_hash, role, is_active)
peps.user_species             → N:N — which species each user can access
peps.playlist                 → Playlists (name, playback_mode)
peps.sound                    → Sound files (type, extension, minio_key, duration)
peps.playlist_sounds          → N:N — ordered sounds in a playlist (play_order)
peps.iot_module               → IoT modules (IP, status, battery, volume, mode) → species, playlist
peps.interaction              → Animal interactions with a module (type, timestamp) → module, sound
peps.observation_measurement  → Observations / measurements (numeric, text or jsonb) → species, user, module
peps.audit_log                → Audit trail used by the archive feature → user
```

### Key design decisions

- All tables live in the **`peps` schema** (not `public`).
- Unquoted identifiers are folded to lowercase by PostgreSQL: `peps.IoT_Module` is stored as `peps.iot_module`. Use lowercase names in JPA (`@Table(name = "iot_module", schema = "peps")`).
- The user table is named **`users`** because `user` is a reserved word in PostgreSQL.
- **Access is species-based**: a user sees the data of the species linked to them in `user_species`. There is no denormalized `owner_role` column anymore, so no trigger is needed to keep copies in sync.
- `playlist_sounds` primary key is `(id_playlist, play_order)`: a sound can appear several times in the same playlist.
- `species.species_name` is `UNIQUE` (alternate key), not part of the primary key.

### Delete behavior

| When deleting… | Effect |
|---|---|
| a **species** | Blocked (`RESTRICT`) if modules or observations reference it |
| a **playlist** | Modules keep existing with `id_playlist = NULL`; its `playlist_sounds` rows are deleted |
| a **sound** | Removed from playlists; interactions keep existing with `id_sound = NULL` |
| a **module** | Interactions and observations are kept with `id_module = NULL` |
| a **user** | `user_species` rows are deleted; observations and audit logs are kept with `id_user = NULL` |

Historical data (interactions, observations, audit logs) is never cascade-deleted.

---

## Demo Users

Created by `10_seed_data.sql`. **Password for all: `admin`** (BCrypt, strength 10).

| Login | Role | Species |
|-------|------|---------|
| `admin` | `admin` | — (all) |
| `dauphin` | `editor` | Dauphin |
| `aras` | `editor` | Ara |

> ⚠️ Demo credentials only. Change them before any non-local deployment.

---

## Archive Test Data

`20_seed_archive_test.sql` uses dates **relative to `NOW()`**, so the dataset stays valid whenever the DB is created:

- **Archivable** (older than 3 months): 9 audit logs, 18 interactions (+ the 4 demo interactions from `10_seed_data.sql`)
- **Not archivable** (recent): 2 audit logs, 2 interactions — used to check the archive does not touch recent data

Do not run it twice on the same database: rows would be duplicated.

---

## Validating After Startup

```bash
docker exec -it peps-database psql -U <DB_USER> -d <DB_NAME>
```

```sql
\dt peps.*                                   -- 10 tables expected

SELECT COUNT(*) FROM peps.users;             -- 3
SELECT COUNT(*) FROM peps.iot_module;        -- 7
SELECT COUNT(*) FROM peps.sound;             -- 10

-- Users and their species
SELECT u.login, u.role, s.species_name
FROM peps.users u
LEFT JOIN peps.user_species us USING (id_user)
LEFT JOIN peps.species s USING (id_species);
```

Replace `<DB_USER>` and `<DB_NAME>` with the values from your `.env` file.

---

## History

The previous schema (role-based ownership with `owner_role`, cascade trigger, patch scripts `02`–`06`, `98`–`101`) was replaced by this version. These scripts patched a live database and are no longer needed for a fresh install. They remain available in the git history.
