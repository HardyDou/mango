INSERT INTO `authorization_subject_role` (
    `id`, `tenant_id`, `subject_id`, `app_code`, `realm`, `actor_type`,
    `party_type`, `party_id`, `role_id`
) VALUES (
    1,
    1,
    1,
    'internal-admin',
    'INTERNAL',
    'INTERNAL_USER',
    'INTERNAL_ORG',
    1,
    1
) ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `app_code` = VALUES(`app_code`),
    `realm` = VALUES(`realm`),
    `actor_type` = VALUES(`actor_type`),
    `party_type` = VALUES(`party_type`),
    `party_id` = VALUES(`party_id`);

INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`
) VALUES (
    9001,
    1,
    'internal-admin',
    1,
    3,
    '全部权限',
    '*:*',
    NULL,
    NULL,
    NULL,
    999,
    1,
    0,
    0,
    0
) ON DUPLICATE KEY UPDATE
    `menu_name` = VALUES(`menu_name`),
    `menu_code` = VALUES(`menu_code`),
    `status` = VALUES(`status`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
VALUES (9001, 1, 1, 9001)
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
