-- Release 1 is an ADDITIVE migration for the existing FoodSave schema.
-- DO NOT infer real food safety dates from expires_at/pickup_end; they have
-- different semantics. Existing rows stay NULL and fail-closed until corrected.
DO $$
BEGIN
  IF to_regclass('public.food_listings') IS NULL THEN
    RAISE EXCEPTION 'FoodSave schema is missing. Bootstrap an empty development DB first; see RELEASE1_INSTALL.md';
  END IF;
END $$;

ALTER TABLE food_listings
  ADD COLUMN IF NOT EXISTS product_expires_at TIMESTAMP WITHOUT TIME ZONE;
CREATE INDEX IF NOT EXISTS idx_food_listings_product_expiry
  ON food_listings(product_expires_at);

-- Used by @Version to reject stale frontend edits and avoid stock overwrite.
ALTER TABLE food_listings
  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
