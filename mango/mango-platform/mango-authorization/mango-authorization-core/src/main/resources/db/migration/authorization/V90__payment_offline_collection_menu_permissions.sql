INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2824,1,'internal-admin','mango-payment',2800,1,'线下支付','payment:offline-payment','/payment/offline','WalletCards',NULL,5,1,1,0,0,'/payment/offline/collections','payment:offline-payment',NULL,NULL,NOW(),NOW(),'线下收款通道的收款确认、退款处理、银行流水导入和对账匹配入口',0,NULL,NOW(),NULL,NOW()),
  (2825,1,'internal-admin','mango-payment',2824,2,'线下收款订单','payment:offline-collection','/payment/offline/collections','Money','@/views/payment/offline-collections/index.vue',1,1,1,0,0,NULL,'payment:offline-collection:list',NULL,NULL,NOW(),NOW(),'线下转账收款单、对账码、转账备注、凭证和到账确认入口',0,NULL,NOW(),NULL,NOW()),
  (282501,1,'internal-admin','mango-payment',2825,3,'线下收款查询','payment:offline-collection:query',NULL,'Search',NULL,1,1,0,0,0,NULL,'payment:offline-collection:query',NULL,NULL,NOW(),NOW(),'线下收款详情查询权限',0,NULL,NOW(),NULL,NOW()),
  (282502,1,'internal-admin','mango-payment',2825,3,'线下收款确认','payment:offline-collection:confirm',NULL,'CheckCircle',NULL,2,1,0,0,0,NULL,'payment:offline-collection:confirm',NULL,NULL,NOW(),NOW(),'线下收款确认到账权限',0,NULL,NOW(),NULL,NOW()),
  (282503,1,'internal-admin','mango-payment',2825,3,'线下收款退款','payment:offline-collection:refund',NULL,'Undo2',NULL,3,1,0,0,0,NULL,'payment:offline-collection:refund',NULL,NULL,NOW(),NOW(),'线下收款发起线下退款权限',0,NULL,NOW(),NULL,NOW())
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

SET @payment_offline_collection_role_menu_base := (
  SELECT COALESCE(MAX(`id`), 9000)
  FROM `authorization_role_menu`
);

INSERT INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT @payment_offline_collection_role_menu_base := @payment_offline_collection_role_menu_base + 1,
       `role`.`tenant_id`,
       `role`.`id`,
       `menu`.`id`,
       NOW(),
       NULL,
       NOW(),
       NULL,
       NOW()
FROM `authorization_role` `role`
JOIN `authorization_menu` `menu`
  ON `menu`.`app_code` = `role`.`app_code`
WHERE `role`.`app_code` = 'internal-admin'
  AND `role`.`role_code` = 'ROLE_ADMIN'
  AND `role`.`status` = 1
  AND `menu`.`id` IN (2824, 2825, 282501, 282502, 282503)
  AND NOT EXISTS (
    SELECT 1
    FROM `authorization_role_menu` `role_menu`
    WHERE `role_menu`.`role_id` = `role`.`id`
      AND `role_menu`.`menu_id` = `menu`.`id`
  );

INSERT IGNORE INTO `frontend_menu_runtime_config`
  (`id`, `menu_id`, `app_code`, `page_type`, `create_time`, `update_time`)
SELECT `menu`.`id`,
       `menu`.`id`,
       `menu`.`app_code`,
       CASE
         WHEN `menu`.`menu_type` = 3 THEN 'BUTTON'
         ELSE 'LOCAL_ROUTE'
       END,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM `authorization_menu` `menu`
WHERE `menu`.`id` IN (2824, 2825, 282501, 282502, 282503);
