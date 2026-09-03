CREATE TABLE driver_location_outbox (
                                        id UUID PRIMARY KEY,
                                        event_type VARCHAR(100) NOT NULL,
                                        aggregate_id BIGINT NOT NULL,
                                        payload JSONB NOT NULL,

                                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                        attempt_count INTEGER NOT NULL DEFAULT 0,
                                        next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        locked_until TIMESTAMPTZ,
                                        published_at TIMESTAMPTZ,
                                        last_error TEXT,

                                        created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_driver_location_outbox_pending
    ON driver_location_outbox (status, next_attempt_at, created_at)
    WHERE status = 'PENDING';