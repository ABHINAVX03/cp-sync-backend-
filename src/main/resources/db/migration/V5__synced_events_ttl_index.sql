-- Index to make the cleanup query fast (filters by synced_at).
-- The @Scheduled cleanup job runs nightly and deletes rows older than 90 days.
CREATE INDEX IF NOT EXISTS idx_synced_events_synced_at
    ON synced_events(synced_at);