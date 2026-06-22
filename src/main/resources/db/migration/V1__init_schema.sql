-- V1__init_schema.sql

CREATE TABLE users (
                       id              BIGSERIAL PRIMARY KEY,
                       google_id       VARCHAR(255) UNIQUE NOT NULL,
                       email           VARCHAR(255) UNIQUE NOT NULL,
                       name            VARCHAR(255),
                       access_token    TEXT,           -- AES encrypted
                       refresh_token   TEXT,           -- AES encrypted
                       token_expiry    TIMESTAMP,
                       active          BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMP NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE user_platform_preferences (
                                           id          BIGSERIAL PRIMARY KEY,
                                           user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           platform    VARCHAR(50) NOT NULL,   -- 'CODEFORCES', 'LEETCODE', 'ATCODER', 'CODECHEF'
                                           enabled     BOOLEAN NOT NULL DEFAULT TRUE,
                                           UNIQUE (user_id, platform)
);

CREATE TABLE synced_events (
                               id              BIGSERIAL PRIMARY KEY,
                               user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               contest_key     VARCHAR(255) NOT NULL,  -- e.g. "CODEFORCES_1234"
                               google_event_id VARCHAR(255),           -- so you can update/delete later
                               synced_at       TIMESTAMP NOT NULL DEFAULT now(),
                               UNIQUE (user_id, contest_key)
);

CREATE INDEX idx_synced_events_user ON synced_events(user_id);
CREATE INDEX idx_platform_prefs_user ON user_platform_preferences(user_id);