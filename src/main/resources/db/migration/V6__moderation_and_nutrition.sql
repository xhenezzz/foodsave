-- Business applications: admin approval with an advisory AI check.
-- Existing businesses were already working, so they start as APPROVED.
ALTER TABLE business_profiles
    ADD COLUMN IF NOT EXISTS business_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS bin VARCHAR(12),
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ai_verdict VARCHAR(16),
    ADD COLUMN IF NOT EXISTS ai_notes VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE business_profiles ALTER COLUMN status SET DEFAULT 'PENDING';

-- Listing moderation and optional nutrition facts per 100 g.
-- Existing listings were already published, so they start as APPROVED.
ALTER TABLE food_listings
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS moderation_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS calories NUMERIC(7, 2),
    ADD COLUMN IF NOT EXISTS proteins NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS fats NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS carbs NUMERIC(6, 2);
ALTER TABLE food_listings ALTER COLUMN moderation_status SET DEFAULT 'PENDING';

CREATE INDEX IF NOT EXISTS ix_business_profiles_status ON business_profiles(status);
CREATE INDEX IF NOT EXISTS ix_food_listings_moderation_status ON food_listings(moderation_status);
