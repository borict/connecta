CREATE TABLE conversations (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID
);

CREATE INDEX idx_conversations_updated ON conversations (updated_at DESC);

CREATE TABLE conversation_participants (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conversation_participants_user ON conversation_participants (user_id);

CREATE TABLE direct_pairs (
    user_a_id       UUID NOT NULL,
    user_b_id       UUID NOT NULL,
    conversation_id UUID NOT NULL UNIQUE REFERENCES conversations(id) ON DELETE CASCADE,
    PRIMARY KEY (user_a_id, user_b_id),
    CONSTRAINT chk_direct_pairs_sorted CHECK (user_a_id < user_b_id)
);

CREATE TABLE messages (
    id              UUID PRIMARY KEY,
    conversation_id UUID          NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID          NOT NULL,
    content         VARCHAR(2000) NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by      UUID
);

CREATE INDEX idx_messages_conversation_created ON messages (conversation_id, created_at DESC);

CREATE TABLE conversation_reads (
    conversation_id UUID        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL,
    last_read_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id)
);
