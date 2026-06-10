INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (281601,1,'internal-admin','mango-payment',2816,3,'通道签约详情','payment:channel-contract:query',NULL,'Search',NULL,1,1,0,0,0,NULL,'payment:channel-contract:query',NULL,NULL,NOW(),NOW(),'通道签约配置详情查询权限',0,NULL,NOW(),NULL,NOW()),
  (281602,1,'internal-admin','mango-payment',2816,3,'通道签约新增','payment:channel-contract:add',NULL,'CirclePlus',NULL,2,1,0,0,0,NULL,'payment:channel-contract:add',NULL,NULL,NOW(),NOW(),'通道签约配置新增权限',0,NULL,NOW(),NULL,NOW()),
  (281603,1,'internal-admin','mango-payment',2816,3,'通道签约编辑','payment:channel-contract:edit',NULL,'Edit',NULL,3,1,0,0,0,NULL,'payment:channel-contract:edit',NULL,NULL,NOW(),NOW(),'通道签约配置编辑权限',0,NULL,NOW(),NULL,NOW()),
  (281604,1,'internal-admin','mango-payment',2816,3,'通道签约删除','payment:channel-contract:delete',NULL,'Delete',NULL,4,1,0,0,0,NULL,'payment:channel-contract:delete',NULL,NULL,NOW(),NOW(),'通道签约配置受控删除权限',0,NULL,NOW(),NULL,NOW()),
  (281605,1,'internal-admin','mango-payment',2816,3,'证书到期提醒','payment:channel-contract:certificate-expiry',NULL,'Bell',NULL,5,1,0,0,0,NULL,'payment:channel-contract:certificate-expiry',NULL,NULL,NOW(),NOW(),'通道签约证书到期提醒查询权限',0,NULL,NOW(),NULL,NOW()),
  (281606,1,'internal-admin','mango-payment',2816,3,'证书轮换登记','payment:channel-contract:certificate-rotate',NULL,'RefreshCw',NULL,6,1,0,0,0,NULL,'payment:channel-contract:certificate-rotate',NULL,NULL,NOW(),NOW(),'通道签约证书轮换登记权限',0,NULL,NOW(),NULL,NOW())
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

SET @payment_channel_contract_role_menu_base := (
  SELECT COALESCE(MAX(`id`), 9000)
  FROM `authorization_role_menu`
);

INSERT INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT @payment_channel_contract_role_menu_base := @payment_channel_contract_role_menu_base + 1,
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
  AND `menu`.`id` IN (281601, 281602, 281603, 281604, 281605, 281606)
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
WHERE `menu`.`id` IN (281601, 281602, 281603, 281604, 281605, 281606);
