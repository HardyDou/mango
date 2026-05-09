INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(20, 1, 'internal-admin', 2, 2, '用户管理', 'system:user', '/system/user', '@/views/system/user/index.vue', 'User', 1, 1, 1, 0, 0, NULL, 'system:user:list', '身份账号管理'),
(2000, 1, 'internal-admin', 20, 3, '查询用户列表', 'system:user:list', NULL, NULL, NULL, 0, 1, 0, 0, 0, NULL, 'system:user:list', '用户列表查询权限'),
(2001, 1, 'internal-admin', 20, 3, '查询用户', 'system:user:query', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'system:user:query', '用户详情查询权限'),
(2002, 1, 'internal-admin', 20, 3, '新增用户', 'system:user:add', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:user:add', '用户新增权限'),
(2003, 1, 'internal-admin', 20, 3, '修改用户', 'system:user:edit', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:user:edit', '用户修改权限'),
(2004, 1, 'internal-admin', 20, 3, '删除用户', 'system:user:delete', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'system:user:delete', '用户删除权限'),
(2005, 1, 'internal-admin', 20, 3, '修改用户状态', 'system:user:status', NULL, NULL, NULL, 5, 1, 0, 0, 0, NULL, 'system:user:status', '用户启用停用权限'),
(2006, 1, 'internal-admin', 20, 3, '重置用户密码', 'system:user:reset-password', NULL, NULL, NULL, 6, 1, 0, 0, 0, NULL, 'system:user:reset-password', '用户密码重置权限'),
(2007, 1, 'internal-admin', 20, 3, '分配用户角色', 'system:user:assign-role', NULL, NULL, NULL, 7, 1, 0, 0, 0, NULL, 'authorization:role:assign', '用户角色分配权限')
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

UPDATE `authorization_menu`
SET `sort` = 2
WHERE `id` = 3
  AND `app_code` = 'internal-admin';

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT 1000000 + `m`.`id`, `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (20, 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007)
WHERE `r`.`tenant_id` = 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + `m`.`id`, `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (20, 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007)
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
