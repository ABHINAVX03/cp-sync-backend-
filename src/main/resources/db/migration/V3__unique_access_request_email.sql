-- Only add the constraint; we’ve already cleaned duplicates manually.
ALTER TABLE access_requests ADD CONSTRAINT IF NOT EXISTS uq_access_requests_email UNIQUE (email);