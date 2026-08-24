CREATE TABLE notifications (
    id                 UUID PRIMARY KEY,
    recipient_id       UUID         NOT NULL,
    actor_id           UUID         NOT NULL,
    type               VARCHAR(50)  NOT NULL,
    resource_type      VARCHAR(50),
    resource_id        UUID,
    message            VARCHAR(500) NOT NULL,
    read               BOOLEAN      NOT NULL DEFAULT FALSE,
    source_message_id  VARCHAR(128),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by         UUID,
    CONSTRAINT chk_notifications_type CHECK (type IN ('LIKE', 'COMMENT', 'FOLLOW', 'MESSAGE')),
    CONSTRAINT chk_notifications_resource_type CHECK (
        resource_type IS NULL OR resource_type IN ('POST', 'COMMENT', 'USER', 'CONVERSATION')
    ),
    CONSTRAINT uk_notifications_source_message_id UNIQUE (source_message_id)
);

CREATE INDEX idx_notif_recipient_created ON notifications (recipient_id, created_at DESC);
CREATE INDEX idx_notif_recipient_unread ON notifications (recipient_id, read) WHERE read = FALSE;
