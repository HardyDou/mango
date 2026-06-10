INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (6, 'internal-admin', 'mango-payment', '支付中心', 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
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
  (2800,1,'internal-admin','mango-payment',0,1,'支付中心','payment','/payment','CreditCard',NULL,8,1,1,0,0,'/payment/applications',NULL,NULL,NULL,NOW(),NOW(),'统一支付平台后台管理',0,NULL,NOW(),NULL,NOW()),
  (2801,1,'internal-admin','mango-payment',2800,2,'应用管理','payment:application','/payment/applications','Box','@/views/payment/applications/index.vue',1,1,1,0,0,NULL,'payment:application:list',NULL,NULL,NOW(),NOW(),'支付接入应用管理',0,NULL,NOW(),NULL,NOW()),
  (2802,1,'internal-admin','mango-payment',2800,2,'企业主体','payment:enterprise-subject','/payment/enterprise-subjects','OfficeBuilding','@/views/payment/enterprise-subjects/index.vue',2,1,1,0,0,NULL,'payment:enterprise-subject:list',NULL,NULL,NOW(),NOW(),'收款主体管理',0,NULL,NOW(),NULL,NOW()),
  (2803,1,'internal-admin','mango-payment',2800,2,'支付通道','payment:channel','/payment/channels','Connection','@/views/payment/channels/index.vue',3,1,1,0,0,NULL,'payment:channel:list',NULL,NULL,NOW(),NOW(),'支付通道配置管理',0,NULL,NOW(),NULL,NOW()),
  (2804,1,'internal-admin','mango-payment',2800,2,'支付方式','payment:method','/payment/methods','Wallet','@/views/payment/methods/index.vue',4,1,1,0,0,NULL,'payment:method:list',NULL,NULL,NOW(),NOW(),'支付方式管理',0,NULL,NOW(),NULL,NOW()),
  (2805,1,'internal-admin','mango-payment',2800,2,'收银台','payment:cashier-config','/payment/cashier-configs','Tickets','@/views/payment/cashier-configs/index.vue',5,1,1,0,0,NULL,'payment:cashier-config:list',NULL,NULL,NOW(),NOW(),'收银台配置管理',0,NULL,NOW(),NULL,NOW()),
  (2806,1,'internal-admin','mango-payment',2800,2,'业务订单','payment:business-order','/payment/business-orders','Document','@/views/payment/business-orders/index.vue',6,1,1,0,0,NULL,'payment:business-order:list',NULL,NULL,NOW(),NOW(),'业务订单查询',0,NULL,NOW(),NULL,NOW()),
  (2807,1,'internal-admin','mango-payment',2800,2,'支付订单','payment:payment-order','/payment/payment-orders','Money','@/views/payment/payment-orders/index.vue',7,1,1,0,0,NULL,'payment:payment-order:list',NULL,NULL,NOW(),NOW(),'支付订单查询',0,NULL,NOW(),NULL,NOW()),
  (2808,1,'internal-admin','mango-payment',2800,2,'退款订单','payment:refund-order','/payment/refund-orders','RefreshLeft','@/views/payment/refund-orders/index.vue',8,1,1,0,0,NULL,'payment:refund-order:list',NULL,NULL,NOW(),NOW(),'退款订单查询',0,NULL,NOW(),NULL,NOW()),
  (2809,1,'internal-admin','mango-payment',2800,2,'交易流水','payment:transaction-flow','/payment/transaction-flows','List','@/views/payment/transaction-flows/index.vue',9,1,1,0,0,NULL,'payment:transaction-flow:list',NULL,NULL,NOW(),NOW(),'支付和退款流水查询',0,NULL,NOW(),NULL,NOW()),
  (2810,1,'internal-admin','mango-payment',2800,2,'异常订单','payment:exception-order','/payment/exception-orders','Warning','@/views/payment/exception-orders/index.vue',10,1,1,0,0,NULL,'payment:exception-order:list',NULL,NULL,NOW(),NOW(),'异常订单处理',0,NULL,NOW(),NULL,NOW()),
  (2811,1,'internal-admin','mango-payment',2800,2,'通知记录','payment:notification-record','/payment/notification-records','Bell','@/views/payment/notification-records/index.vue',11,1,1,0,0,NULL,'payment:notification-record:list',NULL,NULL,NOW(),NOW(),'支付通知记录',0,NULL,NOW(),NULL,NOW()),
  (2812,1,'internal-admin','mango-payment',2800,2,'对账管理','payment:reconciliation','/payment/reconciliations','Files','@/views/payment/reconciliations/index.vue',12,1,1,0,0,NULL,'payment:reconciliation:list',NULL,NULL,NOW(),NOW(),'对账任务和批次管理',0,NULL,NOW(),NULL,NOW()),
  (2813,1,'internal-admin','mango-payment',2800,2,'差异处理','payment:difference','/payment/differences','Operation','@/views/payment/differences/index.vue',13,1,1,0,0,NULL,'payment:difference:list',NULL,NULL,NOW(),NOW(),'对账差异处理',0,NULL,NOW(),NULL,NOW()),
  (2814,1,'internal-admin','mango-payment',2800,2,'结算汇总','payment:settlement-summary','/payment/settlement-summaries','DataAnalysis','@/views/payment/settlement-summaries/index.vue',14,1,1,0,0,NULL,'payment:settlement-summary:list',NULL,NULL,NOW(),NOW(),'结算汇总查询',0,NULL,NOW(),NULL,NOW()),
  (2815,1,'internal-admin','mango-payment',2800,2,'操作审计','payment:operation-audit','/payment/operation-audits','DocumentChecked','@/views/payment/operation-audits/index.vue',15,1,1,0,0,NULL,'payment:operation-audit:list',NULL,NULL,NOW(),NOW(),'支付域操作审计',0,NULL,NOW(),NULL,NOW())
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
