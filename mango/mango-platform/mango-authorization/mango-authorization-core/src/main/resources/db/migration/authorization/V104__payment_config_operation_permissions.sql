INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (280201,1,'internal-admin','mango-payment',2802,3,'签约主体详情','payment:enterprise-subject:query',NULL,'Search',NULL,1,1,0,0,0,NULL,'payment:enterprise-subject:query',NULL,NULL,NOW(),NOW(),'签约主体详情查询权限',0,NULL,NOW(),NULL,NOW()),
  (280202,1,'internal-admin','mango-payment',2802,3,'签约主体新增','payment:enterprise-subject:add',NULL,'CirclePlus',NULL,2,1,0,0,0,NULL,'payment:enterprise-subject:add',NULL,NULL,NOW(),NOW(),'签约主体新增权限',0,NULL,NOW(),NULL,NOW()),
  (280203,1,'internal-admin','mango-payment',2802,3,'签约主体编辑','payment:enterprise-subject:edit',NULL,'Edit',NULL,3,1,0,0,0,NULL,'payment:enterprise-subject:edit',NULL,NULL,NOW(),NOW(),'签约主体编辑权限',0,NULL,NOW(),NULL,NOW()),
  (280204,1,'internal-admin','mango-payment',2802,3,'签约主体删除','payment:enterprise-subject:delete',NULL,'Delete',NULL,4,1,0,0,0,NULL,'payment:enterprise-subject:delete',NULL,NULL,NOW(),NOW(),'签约主体受控删除权限',0,NULL,NOW(),NULL,NOW()),
  (280301,1,'internal-admin','mango-payment',2803,3,'支付通道详情','payment:channel:query',NULL,'Search',NULL,1,1,0,0,0,NULL,'payment:channel:query',NULL,NULL,NOW(),NOW(),'支付通道详情查询权限',0,NULL,NOW(),NULL,NOW()),
  (280302,1,'internal-admin','mango-payment',2803,3,'支付通道新增','payment:channel:add',NULL,'CirclePlus',NULL,2,1,0,0,0,NULL,'payment:channel:add',NULL,NULL,NOW(),NOW(),'支付通道新增权限',0,NULL,NOW(),NULL,NOW()),
  (280303,1,'internal-admin','mango-payment',2803,3,'支付通道编辑','payment:channel:edit',NULL,'Edit',NULL,3,1,0,0,0,NULL,'payment:channel:edit',NULL,NULL,NOW(),NOW(),'支付通道编辑权限',0,NULL,NOW(),NULL,NOW()),
  (280304,1,'internal-admin','mango-payment',2803,3,'支付通道删除','payment:channel:delete',NULL,'Delete',NULL,4,1,0,0,0,NULL,'payment:channel:delete',NULL,NULL,NOW(),NOW(),'支付通道受控删除权限',0,NULL,NOW(),NULL,NOW()),
  (280407,1,'internal-admin','mango-payment',2804,3,'支付方式详情','payment:method:query',NULL,'Search',NULL,7,1,0,0,0,NULL,'payment:method:query',NULL,NULL,NOW(),NOW(),'支付方式详情查询权限',0,NULL,NOW(),NULL,NOW()),
  (280408,1,'internal-admin','mango-payment',2804,3,'支付方式新增','payment:method:add',NULL,'CirclePlus',NULL,8,1,0,0,0,NULL,'payment:method:add',NULL,NULL,NOW(),NOW(),'支付方式新增权限',0,NULL,NOW(),NULL,NOW()),
  (280409,1,'internal-admin','mango-payment',2804,3,'支付方式编辑','payment:method:edit',NULL,'Edit',NULL,9,1,0,0,0,NULL,'payment:method:edit',NULL,NULL,NOW(),NOW(),'支付方式编辑权限',0,NULL,NOW(),NULL,NOW()),
  (280410,1,'internal-admin','mango-payment',2804,3,'支付方式删除','payment:method:delete',NULL,'Delete',NULL,10,1,0,0,0,NULL,'payment:method:delete',NULL,NULL,NOW(),NOW(),'支付方式受控删除权限',0,NULL,NOW(),NULL,NOW()),
  (281706,1,'internal-admin','mango-payment',2817,3,'芒果支付场景控制','payment:mango-pay:scenario-control',NULL,'Operation',NULL,6,1,0,0,0,NULL,'payment:mango-pay:scenario-control',NULL,NULL,NOW(),NOW(),'芒果支付内置通道异常场景控制权限',0,NULL,NOW(),NULL,NOW())
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

SET @payment_config_operation_role_menu_base := (
  SELECT COALESCE(MAX(`id`), 9000)
  FROM `authorization_role_menu`
);

INSERT INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT @payment_config_operation_role_menu_base := @payment_config_operation_role_menu_base + 1,
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
  AND `menu`.`id` IN (280201, 280202, 280203, 280204, 280301, 280302, 280303, 280304, 280407, 280408, 280409, 280410, 281706)
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
WHERE `menu`.`id` IN (280201, 280202, 280203, 280204, 280301, 280302, 280303, 280304, 280407, 280408, 280409, 280410, 281706);
