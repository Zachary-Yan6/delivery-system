CREATE TABLE audit_events (
                              id UUID PRIMARY KEY,

                              actor_user_id BIGINT,
                              actor_username VARCHAR(50) NOT NULL,
                              actor_role VARCHAR(20) NOT NULL,

                              action VARCHAR(100) NOT NULL,
                              entity_type VARCHAR(50) NOT NULL,
                              entity_id BIGINT NOT NULL,

                              details JSONB NOT NULL DEFAULT '{}',

                              trace_id VARCHAR(64),
                              occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_audit_actor
                                  FOREIGN KEY (actor_user_id)
                                      REFERENCES app_users(id)
                                      ON DELETE SET NULL
);

CREATE INDEX idx_audit_events_entity
    ON audit_events(entity_type, entity_id, occurred_at DESC);

CREATE INDEX idx_audit_events_actor
    ON audit_events(actor_user_id, occurred_at DESC);

CREATE INDEX idx_audit_events_trace_id
    ON audit_events(trace_id);