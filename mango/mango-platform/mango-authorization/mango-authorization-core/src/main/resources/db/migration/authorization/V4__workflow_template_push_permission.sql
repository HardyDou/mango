INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2604106,1,'internal-admin','mango-workflow',260401,3,'推送流程','workflow:template:push',NULL,NULL,NULL,6,1,0,0,0,NULL,'workflow:template:push',NULL,NULL,NOW(),NOW(),'将流程模板推送为目标机构流程草稿权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`menu_type` = VALUES(`menu_type`),
`menu_name` = VALUES(`menu_name`),
`menu_code` = VALUES(`menu_code`),
`sort` = VALUES(`sort`),
`status` = VALUES(`status`),
`visible` = VALUES(`visible`),
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`module_code` = VALUES(`module_code`),
`del_flag` = VALUES(`del_flag`),
`update_time` = NOW(),
`updated_at` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(126041006,1,1,2604106,101),
(226041006,1,2,2604106,33);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(5264106,1,1,2604106,NOW(),NULL,NOW(),NULL,NOW()),
(6264106,1,2,2604106,NOW(),NULL,NOW(),NULL,NOW()),
(7264106,1,3,2604106,NOW(),NULL,NOW(),NULL,NOW()),
(8264106,1,4,2604106,NOW(),NULL,NOW(),NULL,NOW());
