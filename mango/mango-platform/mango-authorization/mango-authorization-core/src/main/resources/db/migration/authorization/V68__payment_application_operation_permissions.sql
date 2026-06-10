INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (280101,1,'internal-admin','mango-payment',2801,3,'应用查询','payment:application:query',NULL,'Search',NULL,1,1,0,0,0,NULL,'payment:application:query',NULL,NULL,NOW(),NOW(),'支付应用详情查询权限',0,NULL,NOW(),NULL,NOW()),
  (280102,1,'internal-admin','mango-payment',2801,3,'应用新增','payment:application:add',NULL,'CirclePlus',NULL,2,1,0,0,0,NULL,'payment:application:add',NULL,NULL,NOW(),NOW(),'支付应用新增权限',0,NULL,NOW(),NULL,NOW()),
  (280103,1,'internal-admin','mango-payment',2801,3,'应用编辑','payment:application:edit',NULL,'Edit',NULL,3,1,0,0,0,NULL,'payment:application:edit',NULL,NULL,NOW(),NOW(),'支付应用编辑权限',0,NULL,NOW(),NULL,NOW()),
  (280104,1,'internal-admin','mango-payment',2801,3,'应用删除','payment:application:delete',NULL,'Delete',NULL,4,1,0,0,0,NULL,'payment:application:delete',NULL,NULL,NOW(),NOW(),'支付应用受控逻辑删除权限',0,NULL,NOW(),NULL,NOW())
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

SET @payment_application_role_menu_base := (
  SELECT COALESCE(MAX(`id`), 9000)
  FROM `authorization_role_menu`
);

INSERT INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT @payment_application_role_menu_base := @payment_application_role_menu_base + 1,
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
  AND `menu`.`id` IN (280101, 280102, 280103, 280104)
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
WHERE `menu`.`id` IN (280101, 280102, 280103, 280104);
