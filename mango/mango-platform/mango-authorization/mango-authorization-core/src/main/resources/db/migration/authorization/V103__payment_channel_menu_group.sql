INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2820,1,'internal-admin','mango-payment',2800,1,'应用接入','payment:application-access','/payment/application-access','Grid',NULL,1,1,1,0,0,'/payment/applications',NULL,NULL,NULL,NOW(),NOW(),'支付应用接入和收银台配置',0,NULL,NOW(),NULL,NOW()),
  (2823,1,'internal-admin','mango-payment',2800,1,'支付通道','payment:channel-management','/payment/channel-management','Connection',NULL,2,1,1,0,0,'/payment/enterprise-subjects',NULL,NULL,NULL,NOW(),NOW(),'企业主体、通道签约、通道产品和支付方式管理',0,NULL,NOW(),NULL,NOW()),
  (2821,1,'internal-admin','mango-payment',2800,1,'交易订单','payment:transaction','/payment/transaction','Tickets',NULL,3,1,1,0,0,'/payment/business-orders',NULL,NULL,NULL,NOW(),NOW(),'支付业务订单、支付单、退款单、流水和异常处理',0,NULL,NOW(),NULL,NOW()),
  (2822,1,'internal-admin','mango-payment',2800,1,'对账结算','payment:settlement','/payment/settlement','DataAnalysis',NULL,4,1,1,0,0,'/payment/notification-records',NULL,NULL,NULL,NOW(),NOW(),'支付通知、对账、差异、结算和审计',0,NULL,NOW(),NULL,NOW()),
  (2816,1,'internal-admin','mango-payment',2823,2,'签约通道','payment:channel-contract','/payment/channel-contracts','Postcard','@/views/payment/channel-contracts/index.vue',2,1,1,0,0,NULL,'payment:channel-contract:list',NULL,NULL,NOW(),NOW(),'企业主体在支付通道下的商户号、AppId、证书、密钥和签约能力',0,NULL,NOW(),NULL,NOW())
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
SET `parent_id` = 2820,
    `sort` = CASE `id`
      WHEN 2801 THEN 1
      WHEN 2805 THEN 2
      ELSE `sort`
    END,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (2801, 2805);

UPDATE `authorization_menu`
SET `parent_id` = 2823,
    `sort` = CASE `id`
      WHEN 2802 THEN 1
      WHEN 2816 THEN 2
      WHEN 2803 THEN 3
      WHEN 2804 THEN 4
      ELSE `sort`
    END,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (2802, 2816, 2803, 2804);

UPDATE `authorization_menu`
SET `parent_id` = 2821,
    `sort` = CASE `id`
      WHEN 2806 THEN 1
      WHEN 2807 THEN 2
      WHEN 2808 THEN 3
      WHEN 2809 THEN 4
      WHEN 2810 THEN 5
      ELSE `sort`
    END,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (2806, 2807, 2808, 2809, 2810);

UPDATE `authorization_menu`
SET `parent_id` = 2822,
    `sort` = CASE `id`
      WHEN 2811 THEN 1
      WHEN 2812 THEN 2
      WHEN 2813 THEN 3
      WHEN 2814 THEN 4
      WHEN 2815 THEN 5
      ELSE `sort`
    END,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (2811, 2812, 2813, 2814, 2815);

UPDATE `authorization_menu`
SET `redirect` = '/payment/application-access',
    `sort` = 8,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2800;
