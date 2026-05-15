INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2602000,1,'internal-admin',2602,3,'发起流程','workflow:process:start',NULL,NULL,NULL,1,1,0,0,0,NULL,'workflow:process:start',NULL,NULL,NOW(),NOW(),'发起已发布流程实例',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE `permissions` = VALUES(`permissions`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `del_flag` = VALUES(`del_flag`);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1101,1,1,2602000,101),
(2054,1,2,2602000,54);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(52602000,1,1,2602000,NOW(),NULL,NOW(),NULL,NOW()),
(62602000,2,2,2602000,NOW(),NULL,NOW(),NULL,NOW()),
(72602000,3,3,2602000,NOW(),NULL,NOW(),NULL,NOW()),
(82602000,4,4,2602000,NOW(),NULL,NOW(),NULL,NOW());
