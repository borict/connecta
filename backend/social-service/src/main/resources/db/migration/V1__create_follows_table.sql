CREATE TABLE follows (
    follower_id UUID        NOT NULL,
    followee_id UUID        NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by  UUID,
    PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT chk_follows_not_self CHECK (follower_id <> followee_id),
    CONSTRAINT chk_follows_status CHECK (status IN ('PENDING', 'ACCEPTED'))
);

CREATE INDEX idx_follows_followee_status ON follows (followee_id, status);
CREATE INDEX idx_follows_follower_status ON follows (follower_id, status);
