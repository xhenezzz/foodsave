-- FS-010: persist the seller-confirmed category suggested by AI.
-- Existing listings remain NULL; no category is guessed for legacy data.
ALTER TABLE food_listings
    ADD COLUMN IF NOT EXISTS category VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_food_listings_category
    ON food_listings(category);
