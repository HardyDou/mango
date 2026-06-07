INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (295402,1,'internal-admin','mango-job',2954,3,'Worker 登记','job:worker:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'job:worker:add',NULL,NULL,NOW(),NOW(),'手动登记远程 Worker 权限',0,NULL,NOW(),NULL,NOW()),
  (295403,1,'internal-admin','mango-job',2954,3,'Worker 状态','job:worker:status',NULL,NULL,NULL,3,1,0,0,0,NULL,'job:worker:status',NULL,NULL,NOW(),NOW(),'禁用、排空、下线和恢复 Worker 权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `permissions` = VALUES(`permissions`),
  `remark` = VALUES(`remark`),
  `del_flag` = VALUES(`del_flag`),
  `update_time` = NOW(),
  `updated_at` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
  (1295402,1,1,295402,111),
  (1295403,1,1,295403,112),
  (2295402,1,2,295402,111),
  (2295403,1,2,295403,112);

INSERT IGNORE INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (5295402,1,1,295402,NOW(),NULL,NOW(),NULL,NOW()),
  (5295403,1,1,295403,NOW(),NULL,NOW(),NULL,NOW());
