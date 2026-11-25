-- Admin moderation: a blocked user can neither log in nor use an existing JWT.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS blocked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS blocked_reason VARCHAR(255);
