UPDATE `authorization_menu`
SET `permissions` = 'calendar:admin:list',
    `remark` = '日历、年度日期和工作日计算管理',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2701;

INSERT INTO `authorization_menu`
(`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(270141,1,'internal-admin','mango-calendar',2701,3,'日历查询','calendar:admin:list',NULL,NULL,NULL,41,1,0,0,0,NULL,'calendar:admin:list',NULL,NULL,NOW(),NOW(),'日历查询权限',0,NULL,NOW(),NULL,NOW()),
(270142,1,'internal-admin','mango-calendar',2701,3,'日历新增','calendar:admin:create',NULL,NULL,NULL,42,1,0,0,0,NULL,'calendar:admin:create',NULL,NULL,NOW(),NOW(),'日历新增权限',0,NULL,NOW(),NULL,NOW()),
(270143,1,'internal-admin','mango-calendar',2701,3,'日历编辑','calendar:admin:edit',NULL,NULL,NULL,43,1,0,0,0,NULL,'calendar:admin:edit',NULL,NULL,NOW(),NOW(),'日历编辑权限',0,NULL,NOW(),NULL,NOW()),
(270144,1,'internal-admin','mango-calendar',2701,3,'日历状态','calendar:admin:status',NULL,NULL,NULL,44,1,0,0,0,NULL,'calendar:admin:status',NULL,NULL,NOW(),NOW(),'日历状态权限',0,NULL,NOW(),NULL,NOW()),
(270145,1,'internal-admin','mango-calendar',2701,3,'日历删除','calendar:admin:delete',NULL,NULL,NULL,45,1,0,0,0,NULL,'calendar:admin:delete',NULL,NULL,NOW(),NOW(),'日历删除权限',0,NULL,NOW(),NULL,NOW()),
(270151,1,'internal-admin','mango-calendar',2701,3,'年度查询','calendar:year:list',NULL,NULL,NULL,51,1,0,0,0,NULL,'calendar:year:list',NULL,NULL,NOW(),NOW(),'年度查询权限',0,NULL,NOW(),NULL,NOW()),
(270152,1,'internal-admin','mango-calendar',2701,3,'年度初始化','calendar:year:init',NULL,NULL,NULL,52,1,0,0,0,NULL,'calendar:year:init',NULL,NULL,NOW(),NOW(),'年度初始化权限',0,NULL,NOW(),NULL,NOW()),
(270153,1,'internal-admin','mango-calendar',2701,3,'年度启停','calendar:year:enabled',NULL,NULL,NULL,53,1,0,0,0,NULL,'calendar:year:enabled',NULL,NULL,NOW(),NOW(),'年度启停权限',0,NULL,NOW(),NULL,NOW()),
(270154,1,'internal-admin','mango-calendar',2701,3,'年度删除','calendar:year:delete',NULL,NULL,NULL,54,1,0,0,0,NULL,'calendar:year:delete',NULL,NULL,NOW(),NOW(),'年度删除权限',0,NULL,NOW(),NULL,NOW()),
(270161,1,'internal-admin','mango-calendar',2701,3,'日期查询','calendar:day:list',NULL,NULL,NULL,61,1,0,0,0,NULL,'calendar:day:list',NULL,NULL,NOW(),NOW(),'日期查询权限',0,NULL,NOW(),NULL,NOW()),
(270162,1,'internal-admin','mango-calendar',2701,3,'日期编辑','calendar:day:edit',NULL,NULL,NULL,62,1,0,0,0,NULL,'calendar:day:edit',NULL,NULL,NOW(),NOW(),'日期编辑权限',0,NULL,NOW(),NULL,NOW()),
(270163,1,'internal-admin','mango-calendar',2701,3,'日期批量设置','calendar:day:batch',NULL,NULL,NULL,63,1,0,0,0,NULL,'calendar:day:batch',NULL,NULL,NOW(),NOW(),'日期批量设置权限',0,NULL,NOW(),NULL,NOW()),
(270164,1,'internal-admin','mango-calendar',2701,3,'日期导入','calendar:day:import',NULL,NULL,NULL,64,1,0,0,0,NULL,'calendar:day:import',NULL,NULL,NOW(),NOW(),'日期导入权限',0,NULL,NOW(),NULL,NOW()),
(270165,1,'internal-admin','mango-calendar',2701,3,'日期删除','calendar:day:delete',NULL,NULL,NULL,65,1,0,0,0,NULL,'calendar:day:delete',NULL,NULL,NOW(),NOW(),'日期删除权限',0,NULL,NOW(),NULL,NOW()),
(270171,1,'internal-admin','mango-calendar',2701,3,'工作日计算','calendar:calculate:query',NULL,NULL,NULL,71,1,0,0,0,NULL,'calendar:calculate:query',NULL,NULL,NOW(),NOW(),'工作日计算权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
`module_code` = VALUES(`module_code`),
`parent_id` = VALUES(`parent_id`),
`menu_type` = VALUES(`menu_type`),
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

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`)
SELECT 100000 + `id`, 1, 1, `id`, `sort`
FROM `authorization_menu`
WHERE `parent_id` = 2701 AND `menu_type` = 3 AND `id` >= 270141;

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`)
SELECT 200000 + `id`, 1, 2, `id`, `sort`
FROM `authorization_menu`
WHERE `parent_id` = 2701 AND `menu_type` = 3 AND `id` >= 270141;

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 500000 + `id`, 1, 1, `id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu`
WHERE `parent_id` = 2701 AND `menu_type` = 3 AND `id` >= 270141;

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 600000 + `id`, 1, 2, `id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu`
WHERE `parent_id` = 2701 AND `menu_type` = 3 AND `id` >= 270141;

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 700000 + `id`, 1, 3, `id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu`
WHERE `parent_id` = 2701 AND `menu_type` = 3 AND `id` >= 270141;

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 800000 + `id`, 1, 4, `id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu`
WHERE `parent_id` = 2701 AND `menu_type` = 3 AND `id` >= 270141;
