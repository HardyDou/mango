INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2816,1,'internal-admin','mango-payment',2823,2,'签约通道','payment:channel-contract','/payment/channel-contracts','Postcard','@/views/payment/channel-contracts/index.vue',2,1,1,0,0,NULL,'payment:channel-contract:list',NULL,NULL,NOW(),NOW(),'企业主体在支付通道下的商户号、AppId、证书、密钥和签约能力',0,NULL,NOW(),NULL,NOW()),
  (2817,1,'internal-admin','mango-payment',2821,2,'退款审批','payment:refund-approval','/payment/refund-approvals','ClipboardCheck','@/views/payment/refund-approvals/index.vue',4,1,1,0,0,NULL,'payment:refund-approval:list',NULL,NULL,NOW(),NOW(),'后台退款审批工作流申请查看入口',0,NULL,NOW(),NULL,NOW())
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
SET `sort` = CASE `id`
    WHEN 2806 THEN 1
    WHEN 2807 THEN 2
    WHEN 2808 THEN 3
    WHEN 2817 THEN 4
    WHEN 2809 THEN 5
    WHEN 2810 THEN 6
    WHEN 2811 THEN 12
    WHEN 2812 THEN 13
    WHEN 2813 THEN 14
    WHEN 2814 THEN 15
    WHEN 2815 THEN 16
    ELSE `sort`
  END,
  `update_time` = NOW(),
  `updated_at` = NOW()
WHERE `id` IN (2806, 2807, 2808, 2817, 2809, 2810, 2811, 2812, 2813, 2814, 2815);

SET @payment_menu_restore_role_menu_base := (
  SELECT COALESCE(MAX(`id`), 9000)
  FROM `authorization_role_menu`
);

INSERT INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT @payment_menu_restore_role_menu_base := @payment_menu_restore_role_menu_base + 1,
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
  AND `menu`.`id` IN (2816, 2817)
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
       'VUE_COMPONENT',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM `authorization_menu` `menu`
WHERE `menu`.`id` IN (2816, 2817);
