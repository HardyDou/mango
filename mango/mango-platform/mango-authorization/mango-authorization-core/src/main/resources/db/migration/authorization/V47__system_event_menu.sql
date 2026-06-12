INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2960,1,'internal-admin','mango-system',1,1,'系统维护','system:maintenance','/system/maintenance','Tools',NULL,7,1,1,0,0,'/system/events',NULL,NULL,NULL,NOW(),NOW(),'系统运行维护、异常补偿和基础设施观测入口',0,NULL,NOW(),NULL,NOW()),
  (2961,1,'internal-admin','mango-system',2960,2,'系统事件','system:event','/system/events','RefreshRight','@/views/system/event/index.vue',1,1,1,0,0,NULL,'system:event:list',NULL,NULL,NOW(),NOW(),'领域事件 Outbox 异常查询和重新投递入口',0,NULL,NOW(),NULL,NOW()),
  (296101,1,'internal-admin','mango-system',2961,3,'事件查询','system:event:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:event:list',NULL,NULL,NOW(),NOW(),'系统事件列表查询权限',0,NULL,NOW(),NULL,NOW()),
  (296102,1,'internal-admin','mango-system',2961,3,'事件详情','system:event:detail',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:event:detail',NULL,NULL,NOW(),NOW(),'系统事件详情查询权限',0,NULL,NOW(),NULL,NOW()),
  (296103,1,'internal-admin','mango-system',2961,3,'重新投递','system:event:reconsume',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:event:reconsume',NULL,NULL,NOW(),NOW(),'系统事件重新投递权限',0,NULL,NOW(),NULL,NOW())
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
  `redirect` = VALUES(`redirect`),
  `permissions` = VALUES(`permissions`),
  `remark` = VALUES(`remark`),
  `del_flag` = VALUES(`del_flag`),
  `update_time` = NOW(),
  `updated_at` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
  (12960,1,1,2960,120),
  (12961,1,1,2961,121),
  (1296101,1,1,296101,122),
  (1296102,1,1,296102,123),
  (1296103,1,1,296103,124),
  (22960,1,2,2960,120),
  (22961,1,2,2961,121),
  (2296101,1,2,296101,122),
  (2296102,1,2,296102,123),
  (2296103,1,2,296103,124);

INSERT IGNORE INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (52960,1,1,2960,NOW(),NULL,NOW(),NULL,NOW()),
  (52961,1,1,2961,NOW(),NULL,NOW(),NULL,NOW()),
  (5296101,1,1,296101,NOW(),NULL,NOW(),NULL,NOW()),
  (5296102,1,1,296102,NOW(),NULL,NOW(),NULL,NOW()),
  (5296103,1,1,296103,NOW(),NULL,NOW(),NULL,NOW());
