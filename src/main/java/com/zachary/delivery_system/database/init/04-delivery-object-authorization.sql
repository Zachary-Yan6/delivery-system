-- Apply this to an already-initialized delivery_db database.
-- Docker runs init scripts automatically only when it creates a new PostgreSQL volume.

ALTER TABLE app_users
    DROP CONSTRAINT IF EXISTS chk_user_role;

ALTER TABLE app_users
    ADD CONSTRAINT chk_user_role
        CHECK (role IN ('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER'));

ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS owner_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_delivery_owner'
    ) THEN
        ALTER TABLE deliveries
            ADD CONSTRAINT fk_delivery_owner
            FOREIGN KEY (owner_id) REFERENCES app_users(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_deliveries_owner_id
    ON deliveries(owner_id);

-- Backfill owner_id for existing deliveries before making it NOT NULL.
-- Example:
-- UPDATE deliveries SET owner_id = 12 WHERE id = 123;
