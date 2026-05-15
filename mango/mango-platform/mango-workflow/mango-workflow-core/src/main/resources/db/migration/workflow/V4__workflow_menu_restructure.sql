INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(24,1,'internal-admin',26,2,'流程管理','system:workflow','/workflow/manage','Operation','@/views/system/workflow-definition/index.vue',3,1,1,0,0,NULL,'system:workflow:list',NULL,NULL,NOW(),NOW(),'流程分组与流程定义管理',0,NULL,NOW(),NULL,NOW()),
(24000,1,'internal-admin',24,3,'查询工作流列表','system:workflow:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:workflow:list',NULL,NULL,NOW(),NOW(),'工作流列表查询权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`menu_name` = VALUES(`menu_name`),
`path` = VALUES(`path`),
`icon` = VALUES(`icon`),
`component` = VALUES(`component`),
`sort` = VALUES(`sort`),
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(100024000,1,1,24000,NOW(),NULL,NOW(),NULL,NOW());
