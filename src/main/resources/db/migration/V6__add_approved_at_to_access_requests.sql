ALTER TABLE access_requests
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
