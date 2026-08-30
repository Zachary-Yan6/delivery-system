BEGIN;

ALTER TABLE deliveries
    ADD COLUMN destination_latitude NUMERIC(9, 6),
    ADD COLUMN destination_longitude NUMERIC(9, 6),

    ADD CONSTRAINT chk_delivery_destination_latitude
        CHECK (
            destination_latitude IS NULL
            OR destination_latitude BETWEEN -90 AND 90
        ),

    ADD CONSTRAINT chk_delivery_destination_longitude
        CHECK (
            destination_longitude IS NULL
            OR destination_longitude BETWEEN -180 AND 180
        );

CREATE TABLE driver_locations (
                                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                                  driver_id BIGINT NOT NULL,
                                  latitude NUMERIC(9, 6) NOT NULL,
                                  longitude NUMERIC(9, 6) NOT NULL,

                                  recorded_at TIMESTAMPTZ NOT NULL,
                                  received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_driver_location_driver
                                      FOREIGN KEY (driver_id) REFERENCES drivers(id),

                                  CONSTRAINT chk_driver_location_latitude
                                      CHECK (latitude BETWEEN -90 AND 90),

                                  CONSTRAINT chk_driver_location_longitude
                                      CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_driver_locations_driver_received_at
    ON driver_locations(driver_id, received_at DESC);

COMMIT;