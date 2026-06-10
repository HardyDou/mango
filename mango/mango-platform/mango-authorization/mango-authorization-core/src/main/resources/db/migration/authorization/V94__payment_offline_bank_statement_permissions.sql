INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (282504,1,'internal-admin','mango-payment',2825,3,'线下银行流水查询','payment:offline-collection:bank-statement:query',NULL,'Search',NULL,4,1,0,0,0,NULL,'payment:offline-collection:bank-statement:query',NULL,NULL,NOW(),NOW(),'线下收款银行流水批次和明细查询权限',0,NULL,NOW(),NULL,NOW()),
  (282505,1,'internal-admin','mango-payment',2825,3,'线下银行流水导入','payment:offline-collection:bank-statement:import',NULL,'Upload',NULL,5,1,0,0,0,NULL,'payment:offline-collection:bank-statement:import',NULL,NULL,NOW(),NOW(),'线下收款银行流水 Excel 导入权限',0,NULL,NOW(),NULL,NOW()),
  (282506,1,'internal-admin','mango-payment',2825,3,'线下银行流水确认','payment:offline-collection:bank-statement:confirm',NULL,'CheckCircle',NULL,6,1,0,0,0,NULL,'payment:offline-collection:bank-statement:confirm',NULL,NULL,NOW(),NOW(),'线下收款银行流水匹配确认权限',0,NULL,NOW(),NULL,NOW()),
  (282507,1,'internal-admin','mango-payment',2825,3,'线下银行流水列表','payment:offline-collection:bank-statement:list',NULL,'List',NULL,7,1,0,0,0,NULL,'payment:offline-collection:bank-statement:list',NULL,NULL,NOW(),NOW(),'线下收款银行流水批次列表权限',0,NULL,NOW(),NULL,NOW())
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

SET @payment_offline_bank_statement_role_menu_base := (
  SELECT COALESCE(MAX(`id`), 9000)
  FROM `authorization_role_menu`
);

INSERT INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT @payment_offline_bank_statement_role_menu_base := @payment_offline_bank_statement_role_menu_base + 1,
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
  AND `menu`.`id` IN (282504, 282505, 282506, 282507)
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
       'BUTTON',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM `authorization_menu` `menu`
WHERE `menu`.`id` IN (282504, 282505, 282506, 282507);
