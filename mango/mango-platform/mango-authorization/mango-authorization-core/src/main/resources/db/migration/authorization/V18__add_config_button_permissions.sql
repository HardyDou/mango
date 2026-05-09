INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(6001, 1, 'internal-admin', 6, 3, '查询系统配置', 'system:config:query', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'system:config:query', '系统配置详情查询权限'),
(6002, 1, 'internal-admin', 6, 3, '新增系统配置', 'system:config:add', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:config:add', '系统配置新增权限'),
(6003, 1, 'internal-admin', 6, 3, '修改系统配置', 'system:config:edit', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:config:edit', '系统配置修改权限'),
(6004, 1, 'internal-admin', 6, 3, '删除系统配置', 'system:config:delete', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'system:config:delete', '系统配置删除权限')
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
SELECT 1000000 + `m`.`id`, `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (6001, 6002, 6003, 6004)
WHERE `r`.`tenant_id` = 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
