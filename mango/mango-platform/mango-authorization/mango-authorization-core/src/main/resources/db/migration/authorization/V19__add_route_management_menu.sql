INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(20, 1, 'internal-admin', 5, 2, '路由管理', 'system:route', '/system/route', '@/views/system/route/index.vue', 'Switch', 4, 1, 1, 0, 0, NULL, 'system:route:list', '平台运行路由配置管理'),
(20001, 1, 'internal-admin', 20, 3, '查询系统路由', 'system:route:query', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'system:route:query', '系统路由详情查询权限'),
(20002, 1, 'internal-admin', 20, 3, '新增系统路由', 'system:route:add', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:route:add', '系统路由新增权限'),
(20003, 1, 'internal-admin', 20, 3, '修改系统路由', 'system:route:edit', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:route:edit', '系统路由修改和排序权限'),
(20004, 1, 'internal-admin', 20, 3, '删除系统路由', 'system:route:delete', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'system:route:delete', '系统路由删除权限')
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
JOIN `authorization_menu` `m` ON `m`.`id` IN (20, 20001, 20002, 20003, 20004)
WHERE `r`.`tenant_id` = 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
