INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (280803,1,'internal-admin','mango-payment',2817,3,'退款审批列表','payment:refund-approval:list',NULL,'ListChecks',NULL,1,1,0,0,0,NULL,'payment:refund-approval:list',NULL,NULL,NOW(),NOW(),'后台退款审批列表权限',0,NULL,NOW(),NULL,NOW()),
  (280804,1,'internal-admin','mango-payment',2817,3,'退款审批详情','payment:refund-approval:query',NULL,'Search',NULL,2,1,0,0,0,NULL,'payment:refund-approval:query',NULL,NULL,NOW(),NOW(),'后台退款审批详情权限',0,NULL,NOW(),NULL,NOW()),
  (280805,1,'internal-admin','mango-payment',2817,3,'创建退款审批','payment:refund-approval:create',NULL,'Plus',NULL,3,1,0,0,0,NULL,'payment:refund-approval:create',NULL,NULL,NOW(),NOW(),'后台发起退款审批权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
  `module_code` = VALUES(`module_code`),
  `parent_id` = VALUES(`parent_id`),
  `menu_type` = VALUES(`menu_type`),
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `permissions` = VALUES(`permissions`),
  `remark` = VALUES(`remark`),
  `del_flag` = VALUES(`del_flag`),
  `update_time` = NOW(),
  `updated_at` = NOW();

SET @refund_approval_role_menu_base := (
  SELECT COALESCE(MAX(`id`), 9000)
  FROM `authorization_role_menu`
);

INSERT INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT @refund_approval_role_menu_base := @refund_approval_role_menu_base + 1,
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
  AND `menu`.`id` IN (280803, 280804, 280805)
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
WHERE `menu`.`id` IN (280803, 280804, 280805);

UPDATE `authorization_menu`
SET `status` = 0,
    `visible` = 0,
    `del_flag` = 1,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 280806
   OR `menu_code` = 'payment:refund-approval:review';
