INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2957,1,'internal-admin','mango-job',2950,2,'告警规则','job:alarm','/job/alarm','Bell','@/views/job/alarm/index.vue',5,1,1,0,0,NULL,'job:alarm:list',NULL,NULL,NOW(),NOW(),'Mango Job 失败实例告警规则管理',0,NULL,NOW(),NULL,NOW()),
  (295701,1,'internal-admin','mango-job',2957,3,'告警查询','job:alarm:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'job:alarm:list',NULL,NULL,NOW(),NOW(),'告警规则列表查询权限',0,NULL,NOW(),NULL,NOW()),
  (295702,1,'internal-admin','mango-job',2957,3,'告警详情','job:alarm:query',NULL,NULL,NULL,2,1,0,0,0,NULL,'job:alarm:query',NULL,NULL,NOW(),NOW(),'告警规则详情查询权限',0,NULL,NOW(),NULL,NOW()),
  (295703,1,'internal-admin','mango-job',2957,3,'新增告警','job:alarm:add',NULL,NULL,NULL,3,1,0,0,0,NULL,'job:alarm:add',NULL,NULL,NOW(),NOW(),'告警规则新增权限',0,NULL,NOW(),NULL,NOW()),
  (295704,1,'internal-admin','mango-job',2957,3,'编辑告警','job:alarm:edit',NULL,NULL,NULL,4,1,0,0,0,NULL,'job:alarm:edit',NULL,NULL,NOW(),NOW(),'告警规则编辑权限',0,NULL,NOW(),NULL,NOW()),
  (295705,1,'internal-admin','mango-job',2957,3,'告警状态','job:alarm:status',NULL,NULL,NULL,5,1,0,0,0,NULL,'job:alarm:status',NULL,NULL,NOW(),NOW(),'告警规则启停权限',0,NULL,NOW(),NULL,NOW()),
  (295706,1,'internal-admin','mango-job',2957,3,'删除告警','job:alarm:delete',NULL,NULL,NULL,6,1,0,0,0,NULL,'job:alarm:delete',NULL,NULL,NOW(),NOW(),'告警规则删除权限',0,NULL,NOW(),NULL,NOW())
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
  `permissions` = VALUES(`permissions`),
  `remark` = VALUES(`remark`),
  `del_flag` = VALUES(`del_flag`),
  `update_time` = NOW(),
  `updated_at` = NOW();

UPDATE `authorization_menu`
SET `sort` = 6,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-job'
  AND `menu_code` = 'job:engine';

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
  (12957,1,1,2957,95),
  (1295701,1,1,295701,113),
  (1295702,1,1,295702,114),
  (1295703,1,1,295703,115),
  (1295704,1,1,295704,116),
  (1295705,1,1,295705,117),
  (1295706,1,1,295706,118),
  (22957,1,2,2957,95),
  (2295701,1,2,295701,113),
  (2295702,1,2,295702,114),
  (2295703,1,2,295703,115),
  (2295704,1,2,295704,116),
  (2295705,1,2,295705,117),
  (2295706,1,2,295706,118);

INSERT IGNORE INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (52957,1,1,2957,NOW(),NULL,NOW(),NULL,NOW()),
  (5295701,1,1,295701,NOW(),NULL,NOW(),NULL,NOW()),
  (5295702,1,1,295702,NOW(),NULL,NOW(),NULL,NOW()),
  (5295703,1,1,295703,NOW(),NULL,NOW(),NULL,NOW()),
  (5295704,1,1,295704,NOW(),NULL,NOW(),NULL,NOW()),
  (5295705,1,1,295705,NOW(),NULL,NOW(),NULL,NOW()),
  (5295706,1,1,295706,NOW(),NULL,NOW(),NULL,NOW());
