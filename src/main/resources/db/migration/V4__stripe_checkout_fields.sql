-- Stripe Embedded Checkout references and currency for FoodSave payments.
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'KZT',
    ADD COLUMN IF NOT EXISTS stripe_checkout_session_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_stripe_checkout_session_id
    ON payments(stripe_checkout_session_id)
    WHERE stripe_checkout_session_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_stripe_payment_intent_id
    ON payments(stripe_payment_intent_id)
    WHERE stripe_payment_intent_id IS NOT NULL;
