INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (294106,1,'internal-admin','mango-notice',2941,3,'消息配置删除','notice:business:delete',NULL,NULL,NULL,6,1,0,0,0,NULL,'notice:business:delete',NULL,NULL,NOW(),NOW(),'消息配置删除权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `permissions` = VALUES(`permissions`),
  `remark` = VALUES(`remark`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = NOW(),
  `updated_at` = NOW();
