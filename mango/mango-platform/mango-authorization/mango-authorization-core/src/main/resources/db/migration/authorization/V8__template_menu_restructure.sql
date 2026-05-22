INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (4, 'internal-admin', 'mango-template', '模板中心模块', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

UPDATE `authorization_menu`
SET `module_code` = 'mango-template',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (29, 2901, 2902, 290100, 290101, 290102, 290103, 290104, 290105, 290106, 290107, 290108, 290200, 290201, 290202, 290203, 290204, 290205);

UPDATE `authorization_menu`
SET `menu_name` = '模板列表',
    `path` = '/template/templates',
    `component` = '@/views/template/templates/index.vue',
    `sort` = 2,
    `remark` = '模板主数据、版本发布、历史版本启用和预览渲染',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2901
  AND `menu_code` = 'template:template';

UPDATE `authorization_menu`
SET `sort` = 1,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2902
  AND `menu_code` = 'template:category';

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2903,1,'internal-admin','mango-template',29,2,'渲染记录','template:render-record','/template/render-records','Tickets','@/views/template/render-records/index.vue',3,1,1,0,0,NULL,'template:render-record:list',NULL,NULL,NOW(),NOW(),'模板同步和异步渲染记录查询',0,NULL,NOW(),NULL,NOW()),
(290300,1,'internal-admin','mango-template',2903,3,'查询渲染记录','template:render-record:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'template:render-record:list',NULL,NULL,NOW(),NOW(),'模板渲染记录列表查询权限',0,NULL,NOW(),NULL,NOW()),
(290301,1,'internal-admin','mango-template',2903,3,'查询渲染详情','template:render-record:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'template:render-record:query',NULL,NULL,NOW(),NOW(),'模板渲染记录详情查询权限',0,NULL,NOW(),NULL,NOW())
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
`del_flag` = VALUES(`del_flag`);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1115,1,1,2903,115),
(1116,1,1,290300,116),
(1117,1,1,290301,117);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 500000 + (`role`.`id` * 10000) + `item`.`menu_id`, `role`.`tenant_id`, `role`.`id`, `item`.`menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_role` `role`
JOIN `authorization_menu_package_item` `item` ON `item`.`package_id` = 1
WHERE `role`.`app_code` = 'internal-admin'
  AND `role`.`role_code` = 'ROLE_ADMIN'
  AND `item`.`menu_id` IN (2903, 290300, 290301);
