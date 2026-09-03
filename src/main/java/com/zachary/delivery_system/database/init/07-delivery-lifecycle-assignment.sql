-- Run this migration manually for an existing delivery_db database.
-- Fresh Docker volumes execute it automatically after the earlier init files.
BEGIN;

ALTER TABLE drivers
    ADD COLUMN IF NOT EXISTS available BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS vehicle_capacity_kg NUMERIC(8, 2) NOT NULL DEFAULT 100.00,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE drivers
    DROP CONSTRAINT IF EXISTS chk_driver_vehicle_capacity;

ALTER TABLE drivers
    ADD CONSTRAINT chk_driver_vehicle_capacity
        CHECK (vehicle_capacity_kg > 0);

ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS pickup_latitude NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS pickup_longitude NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS package_weight_kg NUMERIC(8, 2) NOT NULL DEFAULT 1.00,
    ADD COLUMN IF NOT EXISTS time_window_start TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS time_window_end TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE deliveries
    DROP CONSTRAINT IF EXISTS chk_delivery_status,
    DROP CONSTRAINT IF EXISTS chk_delivery_priority,
    DROP CONSTRAINT IF EXISTS chk_delivery_package_weight,
    DROP CONSTRAINT IF EXISTS chk_delivery_pickup_coordinates,
    DROP CONSTRAINT IF EXISTS chk_delivery_time_window;

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
        )),
    ADD CONSTRAINT chk_delivery_priority
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    ADD CONSTRAINT chk_delivery_package_weight
        CHECK (package_weight_kg > 0),
    ADD CONSTRAINT chk_delivery_pickup_coordinates
        CHECK (
            (pickup_latitude IS NULL AND pickup_longitude IS NULL)
            OR (
                pickup_latitude IS NOT NULL
                AND pickup_longitude IS NOT NULL
                AND
                pickup_latitude BETWEEN -90 AND 90
                AND pickup_longitude BETWEEN -180 AND 180
            )
        ),
    ADD CONSTRAINT chk_delivery_time_window
        CHECK (
            (time_window_start IS NULL AND time_window_end IS NULL)
            OR (
                time_window_start IS NOT NULL
                AND time_window_end IS NOT NULL
                AND time_window_start < time_window_end
            )
        );

CREATE INDEX IF NOT EXISTS idx_drivers_assignment_availability
    ON drivers(active, available);

COMMIT;
