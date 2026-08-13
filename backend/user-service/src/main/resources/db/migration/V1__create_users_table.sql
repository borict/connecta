CREATE TABLE users (
    id                  UUID PRIMARY KEY,
    username            VARCHAR(50)  NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    display_name        VARCHAR(100) NOT NULL,
    bio                 VARCHAR(100),
    profile_picture_url VARCHAR(500),
    date_of_birth       DATE         NOT NULL,
    location            VARCHAR(100),
    gender              VARCHAR(20),
    is_private          BOOLEAN      NOT NULL DEFAULT FALSE,
    role                VARCHAR(20)  NOT NULL DEFAULT 'USER',
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_banned           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by          UUID
);

CREATE INDEX idx_users_username_lower ON users (LOWER(username));
CREATE INDEX idx_users_email_lower ON users (LOWER(email));
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_is_banned ON users (is_banned);
