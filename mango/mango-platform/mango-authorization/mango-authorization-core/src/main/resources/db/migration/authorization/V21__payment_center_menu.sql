INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (6, 'internal-admin', 'mango-payment', '支付模块', 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
  (6, 'internal-admin', 'mango-payment', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (16, 'internal-admin', 'mango-payment', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (26, 'internal-admin', 'mango-payment', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2720,1,'internal-admin','mango-payment',0,1,'支付中心','payment','/payment','CreditCard',NULL,6,1,1,0,0,'/payment/management',NULL,NULL,NULL,NOW(),NOW(),'支付平台能力、商户应用、通道配置和租户收银台入口',0,NULL,NOW(),NULL,NOW()),
  (2721,1,'internal-admin','mango-payment',2720,2,'支付管理','payment:management','/payment/management','Management','payment/management/index',1,1,1,0,0,NULL,'payment:management:list',NULL,NULL,NOW(),NOW(),'支付应用、支付方式、沙箱通道、交易订单和退款订单管理',0,NULL,NOW(),NULL,NOW()),
  (2722,1,'internal-admin','mango-payment',2720,2,'租户收银台','payment:cashier','/payment/cashier','Money','payment/cashier/index',2,1,1,0,0,NULL,'payment:cashier:use',NULL,NULL,NOW(),NOW(),'按租户展示收银台并完成沙箱支付流程',0,NULL,NOW(),NULL,NOW()),
  (272101,1,'internal-admin','mango-payment',2721,3,'支付管理查询','payment:management:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'payment:management:list',NULL,NULL,NOW(),NOW(),'支付管理查询权限',0,NULL,NOW(),NULL,NOW()),
  (272102,1,'internal-admin','mango-payment',2721,3,'支付管理维护','payment:management:write',NULL,NULL,NULL,2,1,0,0,0,NULL,'payment:management:write',NULL,NULL,NOW(),NOW(),'支付管理维护权限',0,NULL,NOW(),NULL,NOW()),
  (272201,1,'internal-admin','mango-payment',2722,3,'收银台使用','payment:cashier:use',NULL,NULL,NULL,1,1,0,0,0,NULL,'payment:cashier:use',NULL,NULL,NOW(),NOW(),'租户收银台使用权限',0,NULL,NOW(),NULL,NOW())
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
  (12720,1,1,2720,50),
  (12721,1,1,2721,51),
  (12722,1,1,2722,52),
  (1272101,1,1,272101,53),
  (1272102,1,1,272102,54),
  (1272201,1,1,272201,55),
  (22720,1,2,2720,50),
  (22721,1,2,2721,51),
  (22722,1,2,2722,52),
  (2272101,1,2,272101,53),
  (2272102,1,2,272102,54),
  (2272201,1,2,272201,55);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
  (52720,1,1,2720,NOW(),NULL,NOW(),NULL,NOW()),
  (52721,1,1,2721,NOW(),NULL,NOW(),NULL,NOW()),
  (52722,1,1,2722,NOW(),NULL,NOW(),NULL,NOW()),
  (5272101,1,1,272101,NOW(),NULL,NOW(),NULL,NOW()),
  (5272102,1,1,272102,NOW(),NULL,NOW(),NULL,NOW()),
  (5272201,1,1,272201,NOW(),NULL,NOW(),NULL,NOW()),
  (62720,1,2,2720,NOW(),NULL,NOW(),NULL,NOW()),
  (62721,1,2,2721,NOW(),NULL,NOW(),NULL,NOW()),
  (62722,1,2,2722,NOW(),NULL,NOW(),NULL,NOW()),
  (6272101,1,2,272101,NOW(),NULL,NOW(),NULL,NOW()),
  (6272102,1,2,272102,NOW(),NULL,NOW(),NULL,NOW()),
  (6272201,1,2,272201,NOW(),NULL,NOW(),NULL,NOW()),
  (72720,1,3,2720,NOW(),NULL,NOW(),NULL,NOW()),
  (72721,1,3,2721,NOW(),NULL,NOW(),NULL,NOW()),
  (72722,1,3,2722,NOW(),NULL,NOW(),NULL,NOW()),
  (7272101,1,3,272101,NOW(),NULL,NOW(),NULL,NOW()),
  (7272102,1,3,272102,NOW(),NULL,NOW(),NULL,NOW()),
  (7272201,1,3,272201,NOW(),NULL,NOW(),NULL,NOW()),
  (82720,1,4,2720,NOW(),NULL,NOW(),NULL,NOW()),
  (82721,1,4,2721,NOW(),NULL,NOW(),NULL,NOW()),
  (82722,1,4,2722,NOW(),NULL,NOW(),NULL,NOW()),
  (8272101,1,4,272101,NOW(),NULL,NOW(),NULL,NOW()),
  (8272102,1,4,272102,NOW(),NULL,NOW(),NULL,NOW()),
  (8272201,1,4,272201,NOW(),NULL,NOW(),NULL,NOW());
