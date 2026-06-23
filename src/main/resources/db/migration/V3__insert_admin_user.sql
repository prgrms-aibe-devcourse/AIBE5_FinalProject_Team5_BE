INSERT INTO `users` (
    `email`,
    `password_hash`,
    `name`,
    `nickname`,
    `role`,
    `provider`,
    `provider_user_id`,
    `profile_image_url`,
    `deleted`,
    `deleted_at`,
    `created_at`,
    `updated_at`
) VALUES (
    'admin@admin.com',
    '$2a$10$9D7pWSM9vh8WhiW3OkjLz.MEqyVRAW4NVwQaZaHcSxp6KG3H3Lsgm',
    '관리자',
    'admin',
    'ADMIN',
    'LOCAL',
    NULL,
    NULL,
    false,
    NULL,
    NOW(),
    NOW()
);
