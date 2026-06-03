-- Helm Seek — PostgreSQL Schema
-- Run once to initialise the database

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- enables gen_random_uuid()

-- ─── Users ────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
                                     id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username        TEXT        NOT NULL UNIQUE,
    password        TEXT        NOT NULL,           -- Argon2 hash
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_read       TIMESTAMPTZ,

    -- Theme
    theme_mode      TEXT        NOT NULL DEFAULT 'light'
    CHECK (theme_mode IN ('light', 'dark')),

    -- Color
    selected_color  TEXT        NOT NULL DEFAULT '#1a73e8,#155ab6',

    -- Hero widget
    hero_enabled        BOOLEAN NOT NULL DEFAULT true,
    hero_mode           TEXT    NOT NULL DEFAULT 'greeting'
    CHECK (hero_mode IN ('clock', 'greeting', 'both', 'none')),
    hero_clock_format   TEXT    NOT NULL DEFAULT '12h'
    CHECK (hero_clock_format IN ('12h', '24h')),
    hero_show_seconds   BOOLEAN NOT NULL DEFAULT false,
    hero_greeting_name  TEXT    NOT NULL DEFAULT ''
    CHECK (char_length(hero_greeting_name) <= 30),

    -- Weather widget
    weather_enabled BOOLEAN          NOT NULL DEFAULT false,
    weather_zip     TEXT             NOT NULL DEFAULT '',
    weather_corner  TEXT             NOT NULL DEFAULT 'top-right'
    CHECK (weather_corner IN ('top-left', 'top-right', 'bottom-left')),
    weather_city    TEXT             NOT NULL DEFAULT '',
    weather_lat     DOUBLE PRECISION NOT NULL DEFAULT 0,
    weather_lng     DOUBLE PRECISION NOT NULL DEFAULT 0,

    -- Quick links (variable-length array of objects — stored as JSONB)
    quick_links_enabled BOOLEAN NOT NULL DEFAULT false,
    quick_links         JSONB   NOT NULL DEFAULT '[]'
    );

-- ─── Sessions ─────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sessions (
                                        id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL DEFAULT now() + interval '30 days'
    );

-- ─── Interaction Log ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS interaction_log (
                                               id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        REFERENCES users(id) ON DELETE SET NULL, -- nullable, logs unauthenticated events too
    ip          TEXT        NOT NULL,
    action      TEXT        NOT NULL,
    metadata    JSONB,                                               -- optional action-specific details
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- ─── Indexes ──────────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_sessions_user_id     ON sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at  ON sessions (expires_at);
CREATE INDEX IF NOT EXISTS idx_interaction_log_user_id  ON interaction_log (user_id);
CREATE INDEX IF NOT EXISTS idx_interaction_log_action   ON interaction_log (action);
CREATE INDEX IF NOT EXISTS idx_interaction_log_created_at ON interaction_log (created_at DESC);
