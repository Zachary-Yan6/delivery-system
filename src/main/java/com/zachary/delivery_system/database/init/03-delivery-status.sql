-- Apply this to an already-initialized delivery_db database.
-- Docker runs it automatically only when PostgreSQL first creates its volume.
ALTER TABLE deliveries
    DROP CONSTRAINT IF EXISTS chk_delivery_status;

ALTER TABLE deliveries
    ADD CONSTRAINT chk_delivery_status
        CHECK (status IN (
            'CREATED',
            'ASSIGNED',
            'ACCEPTED',
            'PICKED_UP',
            'IN_TRANSIT',
            'DELIVERED',
            'FAILED',
            'RETRY',
            'COMPLETED',
            'CANCELLED',
            'RETURNED'
        ));
