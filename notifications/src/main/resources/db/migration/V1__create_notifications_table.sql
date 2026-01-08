CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               event_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               channel VARCHAR(20) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(20) NOT NULL DEFAULT 'NEW',
                               created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_event_id ON notifications(event_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);

CREATE TABLE outbox_events (
                               id BIGSERIAL PRIMARY KEY,
                               event_type VARCHAR(100) NOT NULL,
                               event_data TEXT NOT NULL,
                               published BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_events_published ON outbox_events(published);
CREATE INDEX idx_outbox_events_created_at ON outbox_events(created_at);
