DELETE FROM `authorization_role_menu`
WHERE `menu_id` IN (2604,2604000,2604001,2604002,2604003,2604004,2604005,2604006);

DELETE FROM `authorization_menu_package_item`
WHERE `menu_id` IN (2604,2604000,2604001,2604002,2604003,2604004,2604005,2604006);

DELETE FROM `authorization_menu`
WHERE `id` IN (2604,2604000,2604001,2604002,2604003,2604004,2604005,2604006);

DELETE FROM `authorization_role_menu`
WHERE `menu_id` IN (24000,24003,24004,24005,24006);

DELETE FROM `authorization_menu_package_item`
WHERE `menu_id` IN (24000,24003,24004,24005,24006);

DELETE FROM `authorization_menu`
WHERE `id` IN (24000,24003,24004,24005,24006)
  AND (`menu_code` IS NULL OR `menu_code` LIKE 'system:workflow%');

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2604,1,'internal-admin',26,2,'流程管理','system:workflow','/workflow/manage','Operation','@/views/system/workflow-definition/index.vue',3,1,1,0,0,NULL,'system:workflow:list',NULL,NULL,NOW(),NOW(),'流程分组与流程定义管理',0,NULL,NOW(),NULL,NOW()),
(2604000,1,'internal-admin',2604,3,'查询流程','system:workflow:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:workflow:list',NULL,NULL,NOW(),NOW(),'流程管理列表查询权限',0,NULL,NOW(),NULL,NOW()),
(2604001,1,'internal-admin',2604,3,'查看流程','system:workflow:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:workflow:query',NULL,NULL,NOW(),NOW(),'流程管理详情查询权限',0,NULL,NOW(),NULL,NOW()),
(2604002,1,'internal-admin',2604,3,'新增流程','system:workflow:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:workflow:add',NULL,NULL,NOW(),NOW(),'流程管理新增权限',0,NULL,NOW(),NULL,NOW()),
(2604003,1,'internal-admin',2604,3,'编辑流程','system:workflow:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:workflow:edit',NULL,NULL,NOW(),NOW(),'流程管理编辑权限',0,NULL,NOW(),NULL,NOW()),
(2604004,1,'internal-admin',2604,3,'删除流程','system:workflow:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:workflow:delete',NULL,NULL,NOW(),NOW(),'流程管理删除权限',0,NULL,NOW(),NULL,NOW()),
(2604005,1,'internal-admin',2604,3,'调整状态','system:workflow:status',NULL,NULL,NULL,5,1,0,0,0,NULL,'system:workflow:status',NULL,NULL,NOW(),NOW(),'流程管理状态调整权限',0,NULL,NOW(),NULL,NOW()),
(2604006,1,'internal-admin',2604,3,'发布流程','system:workflow:deploy',NULL,NULL,NULL,6,1,0,0,0,NULL,'system:workflow:deploy',NULL,NULL,NOW(),NOW(),'流程管理发布权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
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

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(12604,1,1,2604,24),
(1260400,1,1,2604000,86),
(1260401,1,1,2604001,87),
(1260402,1,1,2604002,88),
(1260403,1,1,2604003,89),
(1260404,1,1,2604004,90),
(1260405,1,1,2604005,91),
(1260406,1,1,2604006,92),
(22604,1,2,2604,17),
(2260400,1,2,2604000,20),
(2260401,1,2,2604001,21),
(2260402,1,2,2604002,22),
(2260403,1,2,2604003,23),
(2260404,1,2,2604004,24),
(2260405,1,2,2604005,25),
(2260406,1,2,2604006,26);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(52604,1,1,2604,NOW(),NULL,NOW(),NULL,NOW()),
(5260400,1,1,2604000,NOW(),NULL,NOW(),NULL,NOW()),
(5260401,1,1,2604001,NOW(),NULL,NOW(),NULL,NOW()),
(5260402,1,1,2604002,NOW(),NULL,NOW(),NULL,NOW()),
(5260403,1,1,2604003,NOW(),NULL,NOW(),NULL,NOW()),
(5260404,1,1,2604004,NOW(),NULL,NOW(),NULL,NOW()),
(5260405,1,1,2604005,NOW(),NULL,NOW(),NULL,NOW()),
(5260406,1,1,2604006,NOW(),NULL,NOW(),NULL,NOW()),
(62604,1,2,2604,NOW(),NULL,NOW(),NULL,NOW()),
(6260400,1,2,2604000,NOW(),NULL,NOW(),NULL,NOW()),
(6260401,1,2,2604001,NOW(),NULL,NOW(),NULL,NOW()),
(6260402,1,2,2604002,NOW(),NULL,NOW(),NULL,NOW()),
(6260403,1,2,2604003,NOW(),NULL,NOW(),NULL,NOW()),
(6260404,1,2,2604004,NOW(),NULL,NOW(),NULL,NOW()),
(6260405,1,2,2604005,NOW(),NULL,NOW(),NULL,NOW()),
(6260406,1,2,2604006,NOW(),NULL,NOW(),NULL,NOW()),
(72604,1,3,2604,NOW(),NULL,NOW(),NULL,NOW()),
(7260400,1,3,2604000,NOW(),NULL,NOW(),NULL,NOW()),
(7260401,1,3,2604001,NOW(),NULL,NOW(),NULL,NOW()),
(7260402,1,3,2604002,NOW(),NULL,NOW(),NULL,NOW()),
(7260403,1,3,2604003,NOW(),NULL,NOW(),NULL,NOW()),
(7260404,1,3,2604004,NOW(),NULL,NOW(),NULL,NOW()),
(7260405,1,3,2604005,NOW(),NULL,NOW(),NULL,NOW()),
(7260406,1,3,2604006,NOW(),NULL,NOW(),NULL,NOW()),
(82604,1,4,2604,NOW(),NULL,NOW(),NULL,NOW()),
(8260400,1,4,2604000,NOW(),NULL,NOW(),NULL,NOW()),
(8260401,1,4,2604001,NOW(),NULL,NOW(),NULL,NOW()),
(8260402,1,4,2604002,NOW(),NULL,NOW(),NULL,NOW()),
(8260403,1,4,2604003,NOW(),NULL,NOW(),NULL,NOW()),
(8260404,1,4,2604004,NOW(),NULL,NOW(),NULL,NOW()),
(8260405,1,4,2604005,NOW(),NULL,NOW(),NULL,NOW()),
(8260406,1,4,2604006,NOW(),NULL,NOW(),NULL,NOW());
