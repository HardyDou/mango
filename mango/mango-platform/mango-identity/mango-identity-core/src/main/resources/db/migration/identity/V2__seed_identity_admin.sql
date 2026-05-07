INSERT INTO `identity_user` (
    `id`, `username`, `password`, `nickname`, `realm`, `actor_type`,
    `party_type`, `party_id`, `email`, `phone`, `status`
) VALUES (
    1,
    'admin',
    '$2a$10$Hxg9OlCM4Y9kj31WEea/tuiYXtJABkOIlXf/u/b95OQrq8Uj7qbZK',
    'Administrator',
    'INTERNAL',
    'INTERNAL_USER',
    'INTERNAL_ORG',
    1,
    'admin@mango.io',
    '13800000001',
    1
) ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `nickname` = VALUES(`nickname`),
    `realm` = VALUES(`realm`),
    `actor_type` = VALUES(`actor_type`),
    `party_type` = VALUES(`party_type`),
    `party_id` = VALUES(`party_id`),
    `email` = VALUES(`email`),
    `phone` = VALUES(`phone`),
    `status` = VALUES(`status`);
