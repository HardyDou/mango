INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(2, 1, 'internal-admin', 1, 1, '账号权限', 'system:account-access', '/system/account-access', NULL, 'UserFilled', 1, 1, 1, 0, 0, '/system/role', NULL, '用户、角色和授权管理'),
(18, 1, 'internal-admin', 1, 1, '组织人事', 'system:org-hr', '/system/org-hr', NULL, 'Connection', 2, 1, 1, 0, 0, '/system/org', NULL, '租户内组织架构与岗位管理'),
(19, 1, 'internal-admin', 1, 1, '平台运营', 'system:platform-ops', '/system/platform-ops', NULL, 'Platform', 3, 1, 1, 0, 0, '/system/tenant', NULL, '平台级租户、应用和系统元数据管理'),
(5, 1, 'internal-admin', 1, 1, '基础数据', 'system:base-data', '/system/base-data', NULL, 'DataBoard', 4, 1, 1, 0, 0, '/system/dict', NULL, '字典、参数、行政区划等基础数据'),
(8, 1, 'internal-admin', 1, 1, '审计日志', 'system:audit-log', '/system/audit-log', NULL, 'DocumentChecked', 5, 1, 1, 0, 0, '/system/login-log', NULL, '登录与操作审计日志')
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

INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(3000, 1, 'internal-admin', 3, 3, '查询角色列表', 'authorization:role:list', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'authorization:role:list', '角色列表查询权限'),
(3001, 1, 'internal-admin', 3, 3, '查询角色', 'authorization:role:query', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'authorization:role:query', '角色详情与角色授权查询权限'),
(3002, 1, 'internal-admin', 3, 3, '新增角色', 'authorization:role:add', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'authorization:role:add', '角色新增权限'),
(3003, 1, 'internal-admin', 3, 3, '修改角色', 'authorization:role:edit', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'authorization:role:edit', '角色修改权限'),
(3004, 1, 'internal-admin', 3, 3, '删除角色', 'authorization:role:delete', NULL, NULL, NULL, 5, 1, 0, 0, 0, NULL, 'authorization:role:delete', '角色删除权限'),
(3005, 1, 'internal-admin', 3, 3, '分配角色权限', 'authorization:role:assign', NULL, NULL, NULL, 6, 1, 0, 0, 0, NULL, 'authorization:role:assign', '角色菜单与主体角色分配权限'),
(4001, 1, 'internal-admin', 4, 3, '查询菜单', 'system:menu:query', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'system:menu:query', '菜单详情查询权限'),
(4002, 1, 'internal-admin', 4, 3, '新增菜单', 'system:menu:add', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:menu:add', '菜单新增权限'),
(4003, 1, 'internal-admin', 4, 3, '修改菜单', 'system:menu:edit', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:menu:edit', '菜单修改权限'),
(4004, 1, 'internal-admin', 4, 3, '删除菜单', 'system:menu:delete', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'system:menu:delete', '菜单删除权限'),
(9002, 1, 'internal-admin', 9, 3, '查询登录日志列表', 'system:log:login:list', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'system:log:login:list', '登录日志列表查询权限'),
(9003, 1, 'internal-admin', 9, 3, '查询登录日志', 'system:log:login:query', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:log:login:query', '登录日志详情与导出查询权限'),
(10002, 1, 'internal-admin', 10, 3, '查询操作日志列表', 'system:log:operation:list', NULL, NULL, NULL, 1, 1, 0, 0, 0, NULL, 'system:log:operation:list', '操作日志列表查询权限'),
(10003, 1, 'internal-admin', 10, 3, '查询操作日志', 'system:log:operation:query', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:log:operation:query', '操作日志详情查询权限')
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
SET `parent_id` = 2,
    `sort` = 1
WHERE `id` = 3
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 18,
    `sort` = 1
WHERE `id` = 17
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 18,
    `sort` = 2
WHERE `id` = 15
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 19,
    `sort` = 1
WHERE `id` = 12
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 19,
    `sort` = 2
WHERE `id` = 14
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 19,
    `sort` = 3
WHERE `id` = 4
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 5,
    `sort` = 1
WHERE `id` = 7
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 5,
    `sort` = 2
WHERE `id` = 6
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 5,
    `sort` = 3
WHERE `id` = 16
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 8,
    `sort` = 1
WHERE `id` = 9
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `parent_id` = 8,
    `sort` = 2
WHERE `id` = 10
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `status` = 0,
    `visible` = 0,
    `remark` = '已由平台运营目录承接，保留历史 ID 但不再展示'
WHERE `id` = 11
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `status` = 0,
    `visible` = 0,
    `remark` = '已由平台运营目录承接，保留历史 ID 但不再展示'
WHERE `id` = 13
  AND `app_code` = 'internal-admin';

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT 10018, `tenant_id`, `id`, 18
FROM `authorization_role`
WHERE `tenant_id` = 1
  AND `app_code` = 'internal-admin'
  AND `role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT 10019, `tenant_id`, `id`, 19
FROM `authorization_role`
WHERE `tenant_id` = 1
  AND `app_code` = 'internal-admin'
  AND `role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT 1000000 + `m`.`id`, `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (
    3000, 3001, 3002, 3003, 3004, 3005,
    4001, 4002, 4003, 4004,
    9002, 9003, 10002, 10003
)
WHERE `r`.`tenant_id` = 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

DELETE `rm`
FROM `authorization_role_menu` `rm`
WHERE `rm`.`tenant_id` <> 1
  AND `rm`.`menu_id` NOT IN (1, 2, 3, 8, 9, 10, 15, 17, 18, 3000, 3001, 3002, 3003, 3004, 3005, 9002, 9003, 10002, 10003);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + 18, `r`.`tenant_id`, `r`.`id`, 18
FROM `authorization_role` `r`
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + 2, `r`.`tenant_id`, `r`.`id`, 2
FROM `authorization_role` `r`
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + 3, `r`.`tenant_id`, `r`.`id`, 3
FROM `authorization_role` `r`
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + 17, `r`.`tenant_id`, `r`.`id`, 17
FROM `authorization_role` `r`
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + 15, `r`.`tenant_id`, `r`.`id`, 15
FROM `authorization_role` `r`
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + 8, `r`.`tenant_id`, `r`.`id`, 8
FROM `authorization_role` `r`
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + 9, `r`.`tenant_id`, `r`.`id`, 9
FROM `authorization_role` `r`
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + 10, `r`.`tenant_id`, `r`.`id`, 10
FROM `authorization_role` `r`
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + `m`.`id`, `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (3000, 3001, 3002, 3003, 3004, 3005, 9002, 9003, 10002, 10003)
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
