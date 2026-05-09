DELETE FROM `authorization_role_menu`
WHERE `menu_id` IN (20001, 20002, 20003, 20004)
   OR (`tenant_id` <> 1 AND `menu_id` = 20);

DELETE FROM `authorization_menu`
WHERE `id` IN (20001, 20002, 20003, 20004);

INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(20, 1, 'internal-admin', 2, 2, '用户管理', 'system:user', '/system/user', '@/views/system/user/index.vue', 'User', 1, 1, 1, 0, 0, NULL, 'system:user:list', '身份账号管理'),
(21, 1, 'internal-admin', 5, 2, '路由管理', 'system:route', '/system/route', '@/views/system/route/index.vue', 'Switch', 4, 1, 1, 0, 0, NULL, 'system:route:list', '平台运行路由配置管理'),
(21001, 1, 'internal-admin', 21, 3, '查询系统路由', 'system:route:query', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'system:route:query', '系统路由详情查询权限'),
(21002, 1, 'internal-admin', 21, 3, '新增系统路由', 'system:route:add', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:route:add', '系统路由新增权限'),
(21003, 1, 'internal-admin', 21, 3, '修改系统路由', 'system:route:edit', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:route:edit', '系统路由修改和排序权限'),
(21004, 1, 'internal-admin', 21, 3, '删除系统路由', 'system:route:delete', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'system:route:delete', '系统路由删除权限')
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
JOIN `authorization_menu` `m` ON `m`.`id` IN (21, 21001, 21002, 21003, 21004)
WHERE `r`.`tenant_id` = 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT CAST(CONV(SUBSTRING(MD5(CONCAT('tenant-default-role-menu:', `r`.`id`, ':', `m`.`id`)), 1, 15), 16, 10) AS UNSIGNED),
       `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (
    1, 2, 3, 8, 9, 10, 15, 17, 18, 20,
    1500, 1501, 1502, 1503, 1504,
    1700, 1701,
    2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007,
    3000, 3001, 3002, 3003, 3004, 3005,
    9002, 9003, 10002, 10003
)
LEFT JOIN `authorization_role_menu` `existing`
  ON `existing`.`tenant_id` = `r`.`tenant_id`
 AND `existing`.`role_id` = `r`.`id`
 AND `existing`.`menu_id` = `m`.`id`
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
  AND `existing`.`id` IS NULL
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
