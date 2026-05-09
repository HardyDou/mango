INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(1201, 1, 'internal-admin', 12, 3, '查询租户', 'system:tenant:query', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'system:tenant:query', '租户详情查询权限'),
(1202, 1, 'internal-admin', 12, 3, '新增租户', 'system:tenant:add', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:tenant:add', '租户新增权限'),
(1203, 1, 'internal-admin', 12, 3, '修改租户', 'system:tenant:edit', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:tenant:edit', '租户修改权限'),
(1204, 1, 'internal-admin', 12, 3, '删除租户', 'system:tenant:delete', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'system:tenant:delete', '租户删除权限'),
(1401, 1, 'internal-admin', 14, 3, '查询应用', 'authorization:app:query', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'authorization:app:query', '授权应用详情查询权限'),
(1402, 1, 'internal-admin', 14, 3, '新增应用', 'authorization:app:add', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'authorization:app:add', '授权应用新增权限'),
(1403, 1, 'internal-admin', 14, 3, '修改应用', 'authorization:app:edit', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'authorization:app:edit', '授权应用修改权限'),
(1404, 1, 'internal-admin', 14, 3, '删除应用', 'authorization:app:delete', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'authorization:app:delete', '授权应用删除权限')
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
VALUES
(11201, 1, 1, 1201),
(11202, 1, 1, 1202),
(11203, 1, 1, 1203),
(11204, 1, 1, 1204),
(11401, 1, 1, 1401),
(11402, 1, 1, 1402),
(11403, 1, 1, 1403),
(11404, 1, 1, 1404)
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
