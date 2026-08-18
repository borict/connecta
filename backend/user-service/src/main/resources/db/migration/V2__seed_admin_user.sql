-- Seed initial ADMIN account (password: Admin123!).

INSERT INTO users (
    id,
    username,
    email,
    password_hash,
    display_name,
    bio,
    profile_picture_url,
    date_of_birth,
    location,
    gender,
    is_private,
    role,
    is_active,
    is_banned,
    created_at,
    updated_at,
    updated_by
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@connecta.local',
    '$2a$10$6GTanUou8QLx.ne0Z0wuC.qaWegvp5XykjnebC//1a38JJP542iDe',
    'Connecta Admin',
    NULL,
    NULL,
    DATE '1990-01-01',
    NULL,
    NULL,
    FALSE,
    'ADMIN',
    TRUE,
    FALSE,
    NOW(),
    NOW(),
    NULL
);
