UPDATE `authorization_menu`
SET `redirect` = '/notice/message-definition',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2940
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 3,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2942
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 4,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2943
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 5,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2944
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 6,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2945
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2947,1,'internal-admin','mango-notice',2940,2,'发送消息','notice:send-message','/notice/send-message','Promotion','@/views/notice/send-message/index.vue',2,1,1,0,0,NULL,'notice:task:create',NULL,NULL,NOW(),NOW(),'人工触发业务消息发送入口',0,NULL,NOW(),NULL,NOW()),
  (294701,1,'internal-admin','mango-notice',2947,3,'发送消息查询','notice:send-message:view',NULL,NULL,NULL,1,1,0,0,0,NULL,'notice:business:view',NULL,NULL,NOW(),NOW(),'发送消息页面查询业务消息权限',0,NULL,NOW(),NULL,NOW()),
  (294702,1,'internal-admin','mango-notice',2947,3,'发送消息提交','notice:task:create',NULL,NULL,NULL,2,1,0,0,0,NULL,'notice:task:create',NULL,NULL,NOW(),NOW(),'发送消息提交权限',0,NULL,NOW(),NULL,NOW())
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

INSERT INTO `frontend_menu_runtime_config`
  (`id`, `menu_id`, `app_code`, `page_type`, `external_url`, `create_time`, `update_time`)
VALUES
  (2947,2947,'internal-admin','LOCAL_ROUTE',NULL,NOW(),NOW())
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `page_type` = VALUES(`page_type`),
  `external_url` = VALUES(`external_url`),
  `update_time` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
  (12947,1,1,2947,72),
  (22947,1,2,2947,72);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
  (52947,1,1,2947,NOW(),NULL,NOW(),NULL,NOW()),
  (5294701,1,1,294701,NOW(),NULL,NOW(),NULL,NOW()),
  (5294702,1,1,294702,NOW(),NULL,NOW(),NULL,NOW()),
  (62947,1,2,2947,NOW(),NULL,NOW(),NULL,NOW()),
  (6294701,1,2,294701,NOW(),NULL,NOW(),NULL,NOW()),
  (6294702,1,2,294702,NOW(),NULL,NOW(),NULL,NOW());
