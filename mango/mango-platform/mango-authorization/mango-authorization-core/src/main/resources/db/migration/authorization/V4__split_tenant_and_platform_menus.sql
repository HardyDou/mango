UPDATE `authorization_menu`
SET `menu_name` = '租户与组织',
    `menu_code` = 'system:tenant-org',
    `path` = '/system/tenant-org',
    `icon` = 'OfficeBuilding',
    `sort` = 4,
    `redirect` = '/system/tenant',
    `remark` = '系统管理左侧目录'
WHERE `id` = 11
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 11,
    `sort` = 1
WHERE `id` = 12
  AND `app_code` = 'internal-admin';

INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(13, 1, 'internal-admin', 1, 1, '平台应用', 'system:platform', '/system/platform', NULL, 'Box', 5, 1, 1, 0, 0, '/system/app', NULL, '系统管理左侧目录')
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `app_code` = VALUES(`app_code`),
    `parent_id` = VALUES(`parent_id`),
    `menu_type` = VALUES(`menu_type`),
    `menu_name` = VALUES(`menu_name`),
    `menu_code` = VALUES(`menu_code`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `visible` = VALUES(`visible`),
    `keep_alive` = VALUES(`keep_alive`),
    `embedded` = VALUES(`embedded`),
    `redirect` = VALUES(`redirect`),
    `permissions` = VALUES(`permissions`),
    `remark` = VALUES(`remark`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
VALUES (10013, 1, 1, 13)
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
