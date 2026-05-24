INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (4, 'internal-admin', 'mango-calendar', '工作日历模块', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
  (4, 'internal-admin', 'mango-calendar', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (14, 'internal-admin', 'mango-calendar', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (24, 'internal-admin', 'mango-calendar', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2700,1,'internal-admin','mango-calendar',0,1,'平台能力','data','/data','DataAnalysis',NULL,4,1,1,0,0,'/data/calendar',NULL,NULL,NULL,NOW(),NOW(),'工作日历管理入口',0,NULL,NOW(),NULL,NOW()),
(2701,1,'internal-admin','mango-calendar',2700,2,'日历管理','data:calendar','/data/calendar','Calendar','@/views/data/calendar/index.vue',1,1,1,0,0,NULL,'calendar:admin:list',NULL,NULL,NOW(),NOW(),'日历、年度日期和工作日计算管理',0,NULL,NOW(),NULL,NOW()),
(270101,1,'internal-admin','mango-calendar',2701,3,'日历查询','calendar:admin:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'calendar:admin:list',NULL,NULL,NOW(),NOW(),'日历查询权限',0,NULL,NOW(),NULL,NOW())
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
(12700,1,1,2700,40),
(12701,1,1,2701,41),
(1270101,1,1,270101,44),
(22700,1,2,2700,40),
(22701,1,2,2701,41),
(2270101,1,2,270101,44);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(52700,1,1,2700,NOW(),NULL,NOW(),NULL,NOW()),
(52701,1,1,2701,NOW(),NULL,NOW(),NULL,NOW()),
(5270101,1,1,270101,NOW(),NULL,NOW(),NULL,NOW()),
(62700,1,2,2700,NOW(),NULL,NOW(),NULL,NOW()),
(62701,1,2,2701,NOW(),NULL,NOW(),NULL,NOW()),
(6270101,1,2,270101,NOW(),NULL,NOW(),NULL,NOW()),
(72700,1,3,2700,NOW(),NULL,NOW(),NULL,NOW()),
(72701,1,3,2701,NOW(),NULL,NOW(),NULL,NOW()),
(7270101,1,3,270101,NOW(),NULL,NOW(),NULL,NOW()),
(82700,1,4,2700,NOW(),NULL,NOW(),NULL,NOW()),
(82701,1,4,2701,NOW(),NULL,NOW(),NULL,NOW()),
(8270101,1,4,270101,NOW(),NULL,NOW(),NULL,NOW());
