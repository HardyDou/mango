INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (5, 'internal-admin', 'mango-numgen', '编号生成模块', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `module_code` = VALUES(`module_code`),
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (5, 'internal-admin', 'mango-numgen', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (15, 'internal-admin', 'mango-numgen', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (25, 'internal-admin', 'mango-numgen', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2710,1,'internal-admin','mango-numgen',2700,2,'编号规则','data:numgen','/data/numgen','Tickets','@/views/numgen/index.vue',2,1,1,0,0,NULL,'numgen:manage:list',NULL,NULL,NOW(),NOW(),'编号生成器、版本、片段和流水管理',0,NULL,NOW(),NULL,NOW()),
  (271001,1,'internal-admin','mango-numgen',2710,3,'编号规则查询','numgen:manage:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'numgen:manage:list',NULL,NULL,NOW(),NOW(),'编号规则查询权限',0,NULL,NOW(),NULL,NOW()),
  (271002,1,'internal-admin','mango-numgen',2710,3,'编号规则维护','numgen:manage:write',NULL,NULL,NULL,2,1,0,0,0,NULL,'numgen:manage:write',NULL,NULL,NOW(),NOW(),'编号规则维护权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
  `module_code` = VALUES(`module_code`),
  `parent_id` = VALUES(`parent_id`),
  `menu_type` = VALUES(`menu_type`),
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `redirect` = VALUES(`redirect`),
  `permissions` = VALUES(`permissions`),
  `remark` = VALUES(`remark`),
  `del_flag` = VALUES(`del_flag`),
  `update_time` = NOW(),
  `updated_at` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
  (12710,1,1,2710,42),
  (1271001,1,1,271001,43),
  (1271002,1,1,271002,44),
  (22710,1,2,2710,42),
  (2271001,1,2,271001,43),
  (2271002,1,2,271002,44);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
  (52710,1,1,2710,NOW(),NULL,NOW(),NULL,NOW()),
  (5271001,1,1,271001,NOW(),NULL,NOW(),NULL,NOW()),
  (5271002,1,1,271002,NOW(),NULL,NOW(),NULL,NOW()),
  (62710,1,2,2710,NOW(),NULL,NOW(),NULL,NOW()),
  (6271001,1,2,271001,NOW(),NULL,NOW(),NULL,NOW()),
  (6271002,1,2,271002,NOW(),NULL,NOW(),NULL,NOW()),
  (72710,1,3,2710,NOW(),NULL,NOW(),NULL,NOW()),
  (7271001,1,3,271001,NOW(),NULL,NOW(),NULL,NOW()),
  (7271002,1,3,271002,NOW(),NULL,NOW(),NULL,NOW()),
  (82710,1,4,2710,NOW(),NULL,NOW(),NULL,NOW()),
  (8271001,1,4,271001,NOW(),NULL,NOW(),NULL,NOW()),
  (8271002,1,4,271002,NOW(),NULL,NOW(),NULL,NOW());
