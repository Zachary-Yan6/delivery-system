CREATE TABLE IF NOT EXISTS driver_location_outbox (
                                                      id UUID PRIMARY KEY,

                                                      event_type VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload JSONB NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    lock_token UUID,
    locked_until TIMESTAMPTZ,

    published_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_driver_location_outbox_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED')),

    CONSTRAINT chk_driver_location_outbox_attempt_count
    CHECK (attempt_count >= 0)
    );

CREATE INDEX IF NOT EXISTS idx_driver_location_outbox_pending
    ON driver_location_outbox (
    status,
    next_attempt_at,
    created_at
    )
    WHERE status IN ('PENDING', 'PROCESSING');

