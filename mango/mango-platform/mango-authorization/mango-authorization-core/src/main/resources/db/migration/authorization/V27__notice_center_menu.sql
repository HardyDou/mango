INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (8, 'internal-admin', 'mango-notice', '通知中心模块', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `module_code` = VALUES(`module_code`),
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (8, 'internal-admin', 'mango-notice', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18, 'internal-admin', 'mango-notice', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (28, 'internal-admin', 'mango-notice', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2940,1,'internal-admin','mango-notice',0,1,'通知中心','notice','/notice','Bell',NULL,7,1,1,0,0,'/notice/message-definition',NULL,NULL,NULL,NOW(),NOW(),'通知中心入口',0,NULL,NOW(),NULL,NOW()),
  (2941,1,'internal-admin','mango-notice',2940,2,'消息定义','notice:business','/notice/message-definition','Setting','@/views/notice/message-definition/index.vue',1,1,1,0,0,NULL,'notice:business:view',NULL,NULL,NOW(),NOW(),'消息定义、参数和渠道模板配置',0,NULL,NOW(),NULL,NOW()),
  (2942,1,'internal-admin','mango-notice',2940,2,'通知渠道','notice:channel','/notice/channel','Connection','@/views/notice/channel/index.vue',2,1,1,0,0,NULL,'notice:channel:view',NULL,NULL,NOW(),NOW(),'短信、邮件、微信、企微、钉钉通知渠道管理',0,NULL,NOW(),NULL,NOW()),
  (2943,1,'internal-admin','mango-notice',2940,2,'通知计划','notice:task','/notice/task','Tickets','@/views/notice/task/index.vue',3,1,1,0,0,NULL,'notice:task:view',NULL,NULL,NOW(),NOW(),'通知计划列表和详情',0,NULL,NOW(),NULL,NOW()),
  (2944,1,'internal-admin','mango-notice',2940,2,'通知记录','notice:record','/notice/record','Document','@/views/notice/record/index.vue',4,1,1,0,0,NULL,'notice:record:view',NULL,NULL,NOW(),NOW(),'通知记录和失败原因',0,NULL,NOW(),NULL,NOW()),
  (2945,1,'internal-admin','mango-notice',2940,2,'消息中心','notice:site-message','/notice/site-message','Message','@/views/notice/site-message/index.vue',5,1,1,0,0,NULL,'notice:site:view',NULL,NULL,NOW(),NOW(),'我的消息和站内信管理',0,NULL,NOW(),NULL,NOW()),
  (2946,1,'internal-admin','mango-notice',2940,2,'通知偏好','notice:setting','/notice/setting','Tools','@/views/notice/setting/index.vue',6,1,1,0,0,NULL,'notice:setting:view',NULL,NULL,NOW(),NOW(),'通知重试、频控和提醒偏好',0,NULL,NOW(),NULL,NOW()),
  (294101,1,'internal-admin','mango-notice',2941,3,'消息定义查询','notice:business:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:business:view',NULL,NULL,NOW(),NOW(),'消息定义查询权限',0,NULL,NOW(),NULL,NOW()),
  (294102,1,'internal-admin','mango-notice',2941,3,'消息定义创建','notice:business:create',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:business:create',NULL,NULL,NOW(),NOW(),'消息定义创建权限',0,NULL,NOW(),NULL,NOW()),
  (294103,1,'internal-admin','mango-notice',2941,3,'消息定义编辑','notice:business:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'notice:business:edit',NULL,NULL,NOW(),NOW(),'消息定义编辑权限',0,NULL,NOW(),NULL,NOW()),
  (294104,1,'internal-admin','mango-notice',2941,3,'消息定义发布','notice:business:publish',NULL,NULL,NULL,4,1,0,0,0,NULL,'notice:business:publish',NULL,NULL,NOW(),NOW(),'消息定义发布权限',0,NULL,NOW(),NULL,NOW()),
  (294201,1,'internal-admin','mango-notice',2942,3,'通知渠道查询','notice:channel:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:channel:view',NULL,NULL,NOW(),NOW(),'通知渠道查询权限',0,NULL,NOW(),NULL,NOW()),
  (294202,1,'internal-admin','mango-notice',2942,3,'通知渠道创建','notice:channel:create',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:channel:create',NULL,NULL,NOW(),NOW(),'通知渠道创建权限',0,NULL,NOW(),NULL,NOW()),
  (294203,1,'internal-admin','mango-notice',2942,3,'通知渠道编辑','notice:channel:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'notice:channel:edit',NULL,NULL,NOW(),NOW(),'通知渠道编辑权限',0,NULL,NOW(),NULL,NOW()),
  (294301,1,'internal-admin','mango-notice',2943,3,'通知计划查询','notice:task:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:task:view',NULL,NULL,NOW(),NOW(),'通知计划查询权限',0,NULL,NOW(),NULL,NOW()),
  (294302,1,'internal-admin','mango-notice',2943,3,'通知计划创建','notice:task:create',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:task:create',NULL,NULL,NOW(),NOW(),'通知计划创建权限',0,NULL,NOW(),NULL,NOW()),
  (294401,1,'internal-admin','mango-notice',2944,3,'通知记录查询','notice:record:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:record:view',NULL,NULL,NOW(),NOW(),'通知记录查询权限',0,NULL,NOW(),NULL,NOW()),
  (294501,1,'internal-admin','mango-notice',2945,3,'消息中心查询','notice:site:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:site:view',NULL,NULL,NOW(),NOW(),'消息中心查询权限',0,NULL,NOW(),NULL,NOW()),
  (294502,1,'internal-admin','mango-notice',2945,3,'消息发送','notice:site:create',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:site:create',NULL,NULL,NOW(),NOW(),'消息发送权限',0,NULL,NOW(),NULL,NOW()),
  (294503,1,'internal-admin','mango-notice',2945,3,'消息已读','notice:site:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'notice:site:edit',NULL,NULL,NOW(),NOW(),'消息已读权限',0,NULL,NOW(),NULL,NOW()),
  (294504,1,'internal-admin','mango-notice',2945,3,'消息删除','notice:site:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'notice:site:delete',NULL,NULL,NOW(),NOW(),'消息删除权限',0,NULL,NOW(),NULL,NOW()),
  (294601,1,'internal-admin','mango-notice',2946,3,'通知偏好查询','notice:setting:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:setting:view',NULL,NULL,NOW(),NOW(),'通知偏好查询权限',0,NULL,NOW(),NULL,NOW()),
  (294602,1,'internal-admin','mango-notice',2946,3,'通知偏好编辑','notice:setting:edit',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:setting:edit',NULL,NULL,NOW(),NOW(),'通知偏好编辑权限',0,NULL,NOW(),NULL,NOW())
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
  (12940,1,1,2940,70),(12941,1,1,2941,71),(12942,1,1,2942,72),(12943,1,1,2943,73),(12944,1,1,2944,74),(12945,1,1,2945,75),(12946,1,1,2946,76),
  (22940,1,2,2940,70),(22941,1,2,2941,71),(22942,1,2,2942,72),(22943,1,2,2943,73),(22944,1,2,2944,74),(22945,1,2,2945,75),(22946,1,2,2946,76);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
  (52940,1,1,2940,NOW(),NULL,NOW(),NULL,NOW()),(52941,1,1,2941,NOW(),NULL,NOW(),NULL,NOW()),(52942,1,1,2942,NOW(),NULL,NOW(),NULL,NOW()),(52943,1,1,2943,NOW(),NULL,NOW(),NULL,NOW()),(52944,1,1,2944,NOW(),NULL,NOW(),NULL,NOW()),(52945,1,1,2945,NOW(),NULL,NOW(),NULL,NOW()),(52946,1,1,2946,NOW(),NULL,NOW(),NULL,NOW()),
  (62940,1,2,2940,NOW(),NULL,NOW(),NULL,NOW()),(62941,1,2,2941,NOW(),NULL,NOW(),NULL,NOW()),(62942,1,2,2942,NOW(),NULL,NOW(),NULL,NOW()),(62943,1,2,2943,NOW(),NULL,NOW(),NULL,NOW()),(62944,1,2,2944,NOW(),NULL,NOW(),NULL,NOW()),(62945,1,2,2945,NOW(),NULL,NOW(),NULL,NOW()),(62946,1,2,2946,NOW(),NULL,NOW(),NULL,NOW());
