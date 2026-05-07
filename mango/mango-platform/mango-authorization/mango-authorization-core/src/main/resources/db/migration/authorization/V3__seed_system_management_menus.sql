DELETE FROM `authorization_role_menu`
WHERE `role_id` = 1
  AND `menu_id` IN (2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

DELETE FROM `authorization_role_menu`
WHERE `id` BETWEEN 10002 AND 10012;

DELETE FROM `authorization_menu`
WHERE `id` IN (2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(1, 1, 'internal-admin', 0, 1, '系统管理', 'system', '/system', NULL, 'Setting', 1, 1, 1, 0, 0, '/system/permission', NULL, '顶部系统入口'),
(2, 1, 'internal-admin', 1, 1, '权限与组织', 'system:permission', '/system/permission', NULL, 'Lock', 1, 1, 1, 0, 0, '/system/role', NULL, '系统管理左侧目录'),
(3, 1, 'internal-admin', 2, 2, '角色管理', 'system:role', '/system/role', '@/views/system/role/index.vue', 'UserFilled', 1, 1, 1, 0, 0, NULL, 'system:role:list', '角色管理'),
(4, 1, 'internal-admin', 2, 2, '菜单管理', 'system:menu', '/system/menu', '@/views/system/menu/index.vue', 'Menu', 2, 1, 1, 0, 0, NULL, 'system:menu:list', '菜单管理'),
(5, 1, 'internal-admin', 1, 1, '基础配置', 'system:basic', '/system/basic', NULL, 'Tools', 2, 1, 1, 0, 0, '/system/config', NULL, '系统管理左侧目录'),
(6, 1, 'internal-admin', 5, 2, '参数配置', 'system:config', '/system/config', '@/views/system/config/index.vue', 'Setting', 1, 1, 1, 0, 0, NULL, 'system:config:list', '系统参数配置'),
(7, 1, 'internal-admin', 5, 2, '字典管理', 'system:dict', '/system/dict', '@/views/system/dict/index.vue', 'Collection', 2, 1, 1, 0, 0, NULL, 'system:dict:list', '字典类型与字典数据管理'),
(8, 1, 'internal-admin', 1, 1, '运维审计', 'system:audit', '/system/audit', NULL, 'Document', 3, 1, 1, 0, 0, '/system/login-log', NULL, '系统管理左侧目录'),
(9, 1, 'internal-admin', 8, 2, '登录日志', 'system:login-log', '/system/login-log', '@/views/system/login-log/index.vue', 'DocumentChecked', 1, 1, 1, 0, 0, NULL, 'system:login-log:list', '登录日志查询'),
(10, 1, 'internal-admin', 8, 2, '操作日志', 'system:operation-log', '/system/operation-log', '@/views/system/operation-log/index.vue', 'Tickets', 2, 1, 1, 0, 0, NULL, 'system:operation-log:list', '操作日志查询'),
(11, 1, 'internal-admin', 1, 1, '平台应用', 'system:platform', '/system/platform', NULL, 'Box', 4, 1, 1, 0, 0, '/system/tenant', NULL, '系统管理左侧目录'),
(12, 1, 'internal-admin', 11, 2, '租户管理', 'system:tenant', '/system/tenant', '@/views/system/tenant/index.vue', 'OfficeBuilding', 1, 1, 1, 0, 0, NULL, 'system:tenant:list', '租户管理')
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
(10002, 1, 1, 2),
(10003, 1, 1, 3),
(10004, 1, 1, 4),
(10005, 1, 1, 5),
(10006, 1, 1, 6),
(10007, 1, 1, 7),
(10008, 1, 1, 8),
(10009, 1, 1, 9),
(10010, 1, 1, 10),
(10011, 1, 1, 11),
(10012, 1, 1, 12)
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
