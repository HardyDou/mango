INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(29,1,'internal-admin',0,1,'模板中心','template','/template','DocumentCopy',NULL,4,1,1,0,0,'/template/templates',NULL,NULL,NULL,NOW(),NOW(),'模板库、变量定义与渲染能力入口',0,NULL,NOW(),NULL,NOW()),
(2901,1,'internal-admin',29,2,'模板管理','template:template','/template/templates','Document','@/views/template/templates/index.vue',1,1,1,0,0,NULL,'template:template:list',NULL,NULL,NOW(),NOW(),'模板维护、版本发布、变量定义和预览渲染',0,NULL,NOW(),NULL,NOW()),
(2902,1,'internal-admin',29,2,'模板分类','template:category','/template/categories','FolderOpened','@/views/template/categories/index.vue',2,1,1,0,0,NULL,'template:category:list',NULL,NULL,NOW(),NOW(),'模板分类维护',0,NULL,NOW(),NULL,NOW()),
(290100,1,'internal-admin',2901,3,'查询模板列表','template:template:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'template:template:list',NULL,NULL,NOW(),NOW(),'模板列表查询权限',0,NULL,NOW(),NULL,NOW()),
(290101,1,'internal-admin',2901,3,'查询模板详情','template:template:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'template:template:query',NULL,NULL,NOW(),NOW(),'模板详情与版本查询权限',0,NULL,NOW(),NULL,NOW()),
(290102,1,'internal-admin',2901,3,'新增模板','template:template:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'template:template:add',NULL,NULL,NOW(),NOW(),'模板新增权限',0,NULL,NOW(),NULL,NOW()),
(290103,1,'internal-admin',2901,3,'编辑模板','template:template:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'template:template:edit',NULL,NULL,NOW(),NOW(),'模板编辑权限',0,NULL,NOW(),NULL,NOW()),
(290104,1,'internal-admin',2901,3,'启停模板','template:template:status',NULL,NULL,NULL,4,1,0,0,0,NULL,'template:template:status',NULL,NULL,NOW(),NOW(),'模板启停权限',0,NULL,NOW(),NULL,NOW()),
(290105,1,'internal-admin',2901,3,'发布模板版本','template:template:publish',NULL,NULL,NULL,5,1,0,0,0,NULL,'template:template:publish',NULL,NULL,NOW(),NOW(),'模板版本发布权限',0,NULL,NOW(),NULL,NOW()),
(290106,1,'internal-admin',2901,3,'提取模板变量','template:template:extract-variable',NULL,NULL,NULL,6,1,0,0,0,NULL,'template:template:extract-variable',NULL,NULL,NOW(),NOW(),'模板占位变量提取权限',0,NULL,NOW(),NULL,NOW()),
(290107,1,'internal-admin',2901,3,'渲染模板','template:template:render',NULL,NULL,NULL,7,1,0,0,0,NULL,'template:template:render',NULL,NULL,NOW(),NOW(),'模板预览和正式渲染权限',0,NULL,NOW(),NULL,NOW()),
(290108,1,'internal-admin',2901,3,'查询渲染记录','template:render-record:list',NULL,NULL,NULL,8,1,0,0,0,NULL,'template:render-record:list',NULL,NULL,NOW(),NOW(),'模板渲染记录查询权限',0,NULL,NOW(),NULL,NOW()),
(290200,1,'internal-admin',2902,3,'查询模板分类列表','template:category:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'template:category:list',NULL,NULL,NOW(),NOW(),'模板分类列表查询权限',0,NULL,NOW(),NULL,NOW()),
(290201,1,'internal-admin',2902,3,'查询模板分类详情','template:category:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'template:category:query',NULL,NULL,NOW(),NOW(),'模板分类详情查询权限',0,NULL,NOW(),NULL,NOW()),
(290202,1,'internal-admin',2902,3,'新增模板分类','template:category:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'template:category:add',NULL,NULL,NOW(),NOW(),'模板分类新增权限',0,NULL,NOW(),NULL,NOW()),
(290203,1,'internal-admin',2902,3,'编辑模板分类','template:category:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'template:category:edit',NULL,NULL,NOW(),NOW(),'模板分类编辑权限',0,NULL,NOW(),NULL,NOW()),
(290204,1,'internal-admin',2902,3,'启停模板分类','template:category:status',NULL,NULL,NULL,4,1,0,0,0,NULL,'template:category:status',NULL,NULL,NOW(),NOW(),'模板分类启停权限',0,NULL,NOW(),NULL,NOW()),
(290205,1,'internal-admin',2902,3,'删除模板分类','template:category:delete',NULL,NULL,NULL,5,1,0,0,0,NULL,'template:category:delete',NULL,NULL,NOW(),NOW(),'模板分类删除权限',0,NULL,NOW(),NULL,NOW())
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
`redirect` = VALUES(`redirect`),
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1097,1,1,29,97),
(1098,1,1,2901,98),
(1099,1,1,2902,99),
(1100,1,1,290100,100),
(1101,1,1,290101,101),
(1102,1,1,290102,102),
(1103,1,1,290103,103),
(1104,1,1,290104,104),
(1105,1,1,290105,105),
(1106,1,1,290106,106),
(1107,1,1,290107,107),
(1108,1,1,290108,108),
(1109,1,1,290200,109),
(1110,1,1,290201,110),
(1111,1,1,290202,111),
(1112,1,1,290203,112),
(1113,1,1,290204,113),
(1114,1,1,290205,114);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 500000 + (`role`.`id` * 10000) + `item`.`menu_id`, `role`.`tenant_id`, `role`.`id`, `item`.`menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_role` `role`
JOIN `authorization_menu_package_item` `item` ON `item`.`package_id` = 1
WHERE `role`.`app_code` = 'internal-admin'
  AND `role`.`role_code` = 'ROLE_ADMIN'
  AND `item`.`menu_id` IN (29, 2901, 2902, 290100, 290101, 290102, 290103, 290104, 290105, 290106, 290107, 290108, 290200, 290201, 290202, 290203, 290204, 290205);
