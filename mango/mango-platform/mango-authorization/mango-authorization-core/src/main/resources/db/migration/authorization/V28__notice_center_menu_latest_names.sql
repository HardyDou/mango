INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (8, 'internal-admin', 'mango-notice', '通知中心模块', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
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
  (2940,1,'internal-admin','mango-notice',0,1,'通知中心','notice','/notice','Bell',NULL,7,1,1,0,0,'/notice/message-definition',NULL,NULL,NULL,NOW(),NOW(),'统一消息编排中心入口',0,NULL,NOW(),NULL,NOW()),
  (2941,1,'internal-admin','mango-notice',2940,2,'消息定义','notice:message-definition','/notice/message-definition','DocumentChecked','@/views/notice/message-definition/index.vue',1,1,1,0,0,NULL,'notice:business:view',NULL,NULL,NOW(),NOW(),'业务消息、参数、渠道启停和模板配置',0,NULL,NOW(),NULL,NOW()),
  (2942,1,'internal-admin','mango-notice',2940,2,'通知渠道','notice:channel','/notice/channel','Connection','@/views/notice/channel/index.vue',2,1,1,0,0,NULL,'notice:channel:view',NULL,NULL,NOW(),NOW(),'系统可用通知通道账号和结构化接入配置',0,NULL,NOW(),NULL,NOW()),
  (2943,1,'internal-admin','mango-notice',2940,2,'接收设置','notice:receive-setting','/notice/receive-setting','Switch','@/views/notice/receive-setting/index.vue',3,1,1,0,0,NULL,'notice:setting:view',NULL,NULL,NOW(),NOW(),'全局、业务域和单消息维度接收控制',0,NULL,NOW(),NULL,NOW()),
  (2944,1,'internal-admin','mango-notice',2940,2,'发送记录','notice:record','/notice/record','Tickets','@/views/notice/record/index.vue',4,1,1,0,0,NULL,'notice:record:view',NULL,NULL,NOW(),NOW(),'消息发送历史、渲染内容、渠道响应和失败原因',0,NULL,NOW(),NULL,NOW()),
  (2945,1,'internal-admin','mango-notice',2940,2,'失败重试','notice:retry','/notice/retry','RefreshRight','@/views/notice/retry/index.vue',5,1,1,0,0,NULL,'notice:record:view',NULL,NULL,NOW(),NOW(),'失败发送记录和重试处理入口',0,NULL,NOW(),NULL,NOW()),
  (2946,1,'internal-admin','mango-notice',2940,2,'消息中心','notice:site-message','/notice/site-message','Message','@/views/notice/site-message/index.vue',6,1,0,0,0,NULL,'notice:site:view',NULL,NULL,NOW(),NOW(),'右上角小铃铛用户消息入口，不作为后台菜单展示',0,NULL,NOW(),NULL,NOW()),
  (294101,1,'internal-admin','mango-notice',2941,3,'消息定义查询','notice:business:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:business:view',NULL,NULL,NOW(),NOW(),'消息定义查询权限',0,NULL,NOW(),NULL,NOW()),
  (294102,1,'internal-admin','mango-notice',2941,3,'消息定义创建','notice:business:create',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:business:create',NULL,NULL,NOW(),NOW(),'消息定义创建权限',0,NULL,NOW(),NULL,NOW()),
  (294103,1,'internal-admin','mango-notice',2941,3,'消息定义编辑','notice:business:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'notice:business:edit',NULL,NULL,NOW(),NOW(),'消息定义编辑权限',0,NULL,NOW(),NULL,NOW()),
  (294104,1,'internal-admin','mango-notice',2941,3,'消息定义发布','notice:business:publish',NULL,NULL,NULL,4,1,0,0,0,NULL,'notice:business:publish',NULL,NULL,NOW(),NOW(),'消息定义发布权限',0,NULL,NOW(),NULL,NOW()),
  (294105,1,'internal-admin','mango-notice',2941,3,'消息定义启停','notice:business:enable',NULL,NULL,NULL,5,1,0,0,0,NULL,'notice:business:enable',NULL,NULL,NOW(),NOW(),'消息定义启停权限',0,NULL,NOW(),NULL,NOW()),
  (294201,1,'internal-admin','mango-notice',2942,3,'通知渠道查询','notice:channel:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:channel:view',NULL,NULL,NOW(),NOW(),'通知渠道查询权限',0,NULL,NOW(),NULL,NOW()),
  (294202,1,'internal-admin','mango-notice',2942,3,'通知渠道创建','notice:channel:create',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:channel:create',NULL,NULL,NOW(),NOW(),'通知渠道创建权限',0,NULL,NOW(),NULL,NOW()),
  (294203,1,'internal-admin','mango-notice',2942,3,'通知渠道编辑','notice:channel:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'notice:channel:edit',NULL,NULL,NOW(),NOW(),'通知渠道编辑权限',0,NULL,NOW(),NULL,NOW()),
  (294301,1,'internal-admin','mango-notice',2943,3,'接收设置查询','notice:setting:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:setting:view',NULL,NULL,NOW(),NOW(),'接收设置查询权限',0,NULL,NOW(),NULL,NOW()),
  (294302,1,'internal-admin','mango-notice',2943,3,'接收设置编辑','notice:setting:edit',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:setting:edit',NULL,NULL,NOW(),NOW(),'接收设置编辑权限',0,NULL,NOW(),NULL,NOW()),
  (294401,1,'internal-admin','mango-notice',2944,3,'发送记录查询','notice:record:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:record:view',NULL,NULL,NOW(),NOW(),'发送记录查询权限',0,NULL,NOW(),NULL,NOW()),
  (294501,1,'internal-admin','mango-notice',2945,3,'失败重试查询','notice:record:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:record:view',NULL,NULL,NOW(),NOW(),'失败重试查询权限',0,NULL,NOW(),NULL,NOW()),
  (294601,1,'internal-admin','mango-notice',2946,3,'消息中心查询','notice:site:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:site:view',NULL,NULL,NOW(),NOW(),'消息中心查询权限',0,NULL,NOW(),NULL,NOW()),
  (294602,1,'internal-admin','mango-notice',2946,3,'消息发送','notice:site:create',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:site:create',NULL,NULL,NOW(),NOW(),'消息发送权限',0,NULL,NOW(),NULL,NOW()),
  (294603,1,'internal-admin','mango-notice',2946,3,'消息已读','notice:site:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'notice:site:edit',NULL,NULL,NOW(),NOW(),'消息已读权限',0,NULL,NOW(),NULL,NOW()),
  (294604,1,'internal-admin','mango-notice',2946,3,'消息删除','notice:site:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'notice:site:delete',NULL,NULL,NOW(),NOW(),'消息删除权限',0,NULL,NOW(),NULL,NOW())
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

UPDATE `authorization_menu`
SET `status` = 0,
    `visible` = 0,
    `remark` = '旧站内信按钮权限已迁移到右上角消息入口，后台菜单不再展示',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (294502, 294503, 294504);

DELETE FROM `authorization_menu_package_item`
WHERE `tenant_id` = 1
  AND `package_id` IN (1, 2)
  AND `menu_id` IN (2940,2941,2942,2943,2944,2945,2946);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
  (12940,1,1,2940,70),(12941,1,1,2941,71),(12942,1,1,2942,72),(12943,1,1,2943,73),(12944,1,1,2944,74),(12945,1,1,2945,75),
  (22940,1,2,2940,70),(22941,1,2,2941,71),(22942,1,2,2942,72),(22943,1,2,2943,73),(22944,1,2,2944,74),(22945,1,2,2945,75);

DELETE FROM `authorization_role_menu`
WHERE `tenant_id` = 1
  AND `role_id` IN (1, 2)
  AND `menu_id` IN (2940,2941,2942,2943,2944,2945,2946,294101,294102,294103,294104,294105,294201,294202,294203,294301,294302,294401,294501,294502,294503,294504,294601,294602,294603,294604);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
  (52940,1,1,2940,NOW(),NULL,NOW(),NULL,NOW()),(52941,1,1,2941,NOW(),NULL,NOW(),NULL,NOW()),(52942,1,1,2942,NOW(),NULL,NOW(),NULL,NOW()),(52943,1,1,2943,NOW(),NULL,NOW(),NULL,NOW()),(52944,1,1,2944,NOW(),NULL,NOW(),NULL,NOW()),(52945,1,1,2945,NOW(),NULL,NOW(),NULL,NOW()),
  (5294101,1,1,294101,NOW(),NULL,NOW(),NULL,NOW()),(5294102,1,1,294102,NOW(),NULL,NOW(),NULL,NOW()),(5294103,1,1,294103,NOW(),NULL,NOW(),NULL,NOW()),(5294104,1,1,294104,NOW(),NULL,NOW(),NULL,NOW()),(5294105,1,1,294105,NOW(),NULL,NOW(),NULL,NOW()),
  (5294201,1,1,294201,NOW(),NULL,NOW(),NULL,NOW()),(5294202,1,1,294202,NOW(),NULL,NOW(),NULL,NOW()),(5294203,1,1,294203,NOW(),NULL,NOW(),NULL,NOW()),
  (5294301,1,1,294301,NOW(),NULL,NOW(),NULL,NOW()),(5294302,1,1,294302,NOW(),NULL,NOW(),NULL,NOW()),
  (5294401,1,1,294401,NOW(),NULL,NOW(),NULL,NOW()),(5294501,1,1,294501,NOW(),NULL,NOW(),NULL,NOW()),
  (5294601,1,1,294601,NOW(),NULL,NOW(),NULL,NOW()),(5294602,1,1,294602,NOW(),NULL,NOW(),NULL,NOW()),(5294603,1,1,294603,NOW(),NULL,NOW(),NULL,NOW()),(5294604,1,1,294604,NOW(),NULL,NOW(),NULL,NOW()),
  (62940,1,2,2940,NOW(),NULL,NOW(),NULL,NOW()),(62941,1,2,2941,NOW(),NULL,NOW(),NULL,NOW()),(62942,1,2,2942,NOW(),NULL,NOW(),NULL,NOW()),(62943,1,2,2943,NOW(),NULL,NOW(),NULL,NOW()),(62944,1,2,2944,NOW(),NULL,NOW(),NULL,NOW()),(62945,1,2,2945,NOW(),NULL,NOW(),NULL,NOW()),
  (6294101,1,2,294101,NOW(),NULL,NOW(),NULL,NOW()),(6294102,1,2,294102,NOW(),NULL,NOW(),NULL,NOW()),(6294103,1,2,294103,NOW(),NULL,NOW(),NULL,NOW()),(6294104,1,2,294104,NOW(),NULL,NOW(),NULL,NOW()),(6294105,1,2,294105,NOW(),NULL,NOW(),NULL,NOW()),
  (6294201,1,2,294201,NOW(),NULL,NOW(),NULL,NOW()),(6294202,1,2,294202,NOW(),NULL,NOW(),NULL,NOW()),(6294203,1,2,294203,NOW(),NULL,NOW(),NULL,NOW()),
  (6294301,1,2,294301,NOW(),NULL,NOW(),NULL,NOW()),(6294302,1,2,294302,NOW(),NULL,NOW(),NULL,NOW()),
  (6294401,1,2,294401,NOW(),NULL,NOW(),NULL,NOW()),(6294501,1,2,294501,NOW(),NULL,NOW(),NULL,NOW()),
  (6294601,1,2,294601,NOW(),NULL,NOW(),NULL,NOW()),(6294602,1,2,294602,NOW(),NULL,NOW(),NULL,NOW()),(6294603,1,2,294603,NOW(),NULL,NOW(),NULL,NOW()),(6294604,1,2,294604,NOW(),NULL,NOW(),NULL,NOW());

INSERT INTO `frontend_menu_runtime_config`
  (`id`, `menu_id`, `app_code`, `page_type`, `external_url`, `create_time`, `update_time`)
VALUES
  (2940,2940,'internal-admin','LOCAL_ROUTE',NULL,NOW(),NOW()),
  (2941,2941,'internal-admin','LOCAL_ROUTE',NULL,NOW(),NOW()),
  (2942,2942,'internal-admin','LOCAL_ROUTE',NULL,NOW(),NOW()),
  (2943,2943,'internal-admin','LOCAL_ROUTE',NULL,NOW(),NOW()),
  (2944,2944,'internal-admin','LOCAL_ROUTE',NULL,NOW(),NOW()),
  (2945,2945,'internal-admin','LOCAL_ROUTE',NULL,NOW(),NOW()),
  (2946,2946,'internal-admin','LOCAL_ROUTE',NULL,NOW(),NOW())
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `page_type` = VALUES(`page_type`),
  `external_url` = VALUES(`external_url`),
  `update_time` = NOW();
