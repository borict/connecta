CREATE TABLE posts (
    id         UUID PRIMARY KEY,
    author_id  UUID         NOT NULL,
    content    VARCHAR(500) NOT NULL,
    image_url  VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by UUID
);

CREATE INDEX idx_posts_author_created ON posts (author_id, created_at DESC);
CREATE INDEX idx_posts_created ON posts (created_at DESC);

CREATE TABLE likes (
    id         UUID PRIMARY KEY,
    post_id    UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID,
    CONSTRAINT uk_likes_post_user UNIQUE (post_id, user_id)
);

CREATE TABLE comments (
    id         UUID PRIMARY KEY,
    post_id    UUID         NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id  UUID         NOT NULL,
    content    VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by UUID
);

CREATE INDEX idx_comments_post_created ON comments (post_id, created_at DESC);
