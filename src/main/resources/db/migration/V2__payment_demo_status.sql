-- Release 2: allow intermediate mock-checkout status in existing PostgreSQL database.
-- Your database MUST already have the payments table from the original FoodSave schema.
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'payments'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE payments DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE payments
ADD CONSTRAINT payments_status_check
CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));
