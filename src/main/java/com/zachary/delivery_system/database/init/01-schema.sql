CREATE TABLE app_users (
                           id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           username VARCHAR(50) NOT NULL UNIQUE,
                           password_hash VARCHAR(255) NOT NULL,
                           role VARCHAR(20) NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT chk_user_role
                               CHECK (role IN ('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER'))
);

CREATE TABLE drivers (
                         id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         user_id BIGINT NOT NULL UNIQUE,
                         full_name VARCHAR(100) NOT NULL,
                         phone VARCHAR(30) NOT NULL,
                         active BOOLEAN NOT NULL DEFAULT TRUE,
                         available BOOLEAN NOT NULL DEFAULT TRUE,
                         vehicle_capacity_kg NUMERIC(8, 2) NOT NULL DEFAULT 100.00,
                         version BIGINT NOT NULL DEFAULT 0,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT chk_driver_vehicle_capacity
                             CHECK (vehicle_capacity_kg > 0),

                         CONSTRAINT fk_driver_user
                             FOREIGN KEY (user_id) REFERENCES app_users(id)
);

CREATE TABLE deliveries (
                            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            customer_name VARCHAR(100) NOT NULL,
                            customer_phone VARCHAR(30),
                            address VARCHAR(500) NOT NULL,
                            owner_id BIGINT,
                            driver_id BIGINT,
                            pickup_latitude NUMERIC(9, 6),
                            pickup_longitude NUMERIC(9, 6),
                            status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
                            priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                            package_weight_kg NUMERIC(8, 2) NOT NULL DEFAULT 1.00,
                            time_window_start TIMESTAMPTZ,
                            time_window_end TIMESTAMPTZ,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            delivered_at TIMESTAMPTZ,
                            version BIGINT NOT NULL DEFAULT 0,

                            CONSTRAINT chk_delivery_status
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

                            CONSTRAINT chk_delivery_priority
                                CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),

                            CONSTRAINT chk_delivery_package_weight
                                CHECK (package_weight_kg > 0),

                            CONSTRAINT chk_delivery_pickup_coordinates
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

                            CONSTRAINT chk_delivery_time_window
                                CHECK (
                                    (time_window_start IS NULL AND time_window_end IS NULL)
                                    OR (
                                        time_window_start IS NOT NULL
                                        AND time_window_end IS NOT NULL
                                        AND time_window_start < time_window_end
                                    )
                                ),

                            CONSTRAINT fk_delivery_driver
                                FOREIGN KEY (driver_id) REFERENCES drivers(id),

                            CONSTRAINT fk_delivery_owner
                                FOREIGN KEY (owner_id) REFERENCES app_users(id)
);

CREATE INDEX idx_deliveries_driver_id ON deliveries(driver_id);
CREATE INDEX idx_deliveries_owner_id ON deliveries(owner_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);
CREATE INDEX idx_drivers_assignment_availability
    ON drivers(active, available);
