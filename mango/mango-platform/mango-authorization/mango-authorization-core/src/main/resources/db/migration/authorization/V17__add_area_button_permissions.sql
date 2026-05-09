INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(1601, 1, 'internal-admin', 16, 3, '查询行政区划', 'system:area:query', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'system:area:query', '行政区划详情查询权限'),
(1602, 1, 'internal-admin', 16, 3, '新增行政区划', 'system:area:add', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:area:add', '行政区划新增权限'),
(1603, 1, 'internal-admin', 16, 3, '修改行政区划', 'system:area:edit', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:area:edit', '行政区划修改权限'),
(1604, 1, 'internal-admin', 16, 3, '删除行政区划', 'system:area:delete', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'system:area:delete', '行政区划删除权限')
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
JOIN `authorization_menu` `m` ON `m`.`id` IN (1601, 1602, 1603, 1604)
WHERE `r`.`tenant_id` = 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
