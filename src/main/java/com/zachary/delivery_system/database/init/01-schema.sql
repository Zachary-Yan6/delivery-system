CREATE TABLE app_users (
                           id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           username VARCHAR(50) NOT NULL UNIQUE,
                           password_hash VARCHAR(255) NOT NULL,
                           role VARCHAR(20) NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT chk_user_role
                               CHECK (role IN ('DISPATCHER', 'DRIVER'))
);

CREATE TABLE drivers (
                         id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         user_id BIGINT NOT NULL UNIQUE,
                         full_name VARCHAR(100) NOT NULL,
                         phone VARCHAR(30) NOT NULL,
                         active BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_driver_user
                             FOREIGN KEY (user_id) REFERENCES app_users(id)
);

CREATE TABLE deliveries (
                            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            customer_name VARCHAR(100) NOT NULL,
                            customer_phone VARCHAR(30),
                            address VARCHAR(500) NOT NULL,
                            driver_id BIGINT,
                            status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            delivered_at TIMESTAMPTZ,

                            CONSTRAINT chk_delivery_status
                                CHECK (status IN (
                                                  'CREATED',
                                                  'ASSIGNED',
                                                  'IN_TRANSIT',
                                                  'DELIVERED',
                                                  'FAILED'
                                    )),

                            CONSTRAINT fk_delivery_driver
                                FOREIGN KEY (driver_id) REFERENCES drivers(id)
);

CREATE INDEX idx_deliveries_driver_id ON deliveries(driver_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);