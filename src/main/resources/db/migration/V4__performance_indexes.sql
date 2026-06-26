-- Partial index: the scheduler queries active=TRUE users only.
-- With 10k users and 80% active, this avoids a full table scan on every cron run.
CREATE INDEX IF NOT EXISTS idx_users_active_partial
    ON users(id)
    WHERE active = TRUE;

-- Composite index to accelerate the N+1 query in getAllUserProfiles
-- (covered by the existing unique constraint but explicit for clarity)
CREATE INDEX IF NOT EXISTS idx_platform_prefs_enabled
    ON user_platform_preferences(user_id)
    WHERE enabled = TRUE;