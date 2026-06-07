INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (295202,1,'internal-admin','mango-job',2952,3,'同步实例','job:instance:sync',NULL,NULL,NULL,2,1,0,0,0,NULL,'job:instance:sync',NULL,NULL,NOW(),NOW(),'从调度引擎同步任务实例权限',0,NULL,NOW(),NULL,NOW())
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
  (1295202,1,1,295202,109),
  (2295202,1,2,295202,109);

INSERT IGNORE INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (5295202,1,1,295202,NOW(),NULL,NOW(),NULL,NOW());
