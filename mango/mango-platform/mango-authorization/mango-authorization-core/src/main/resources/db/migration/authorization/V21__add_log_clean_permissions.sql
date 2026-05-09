INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(9004, 1, 'internal-admin', 9, 3, '清理登录日志', 'system:log:login:delete', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:log:login:delete', '登录日志清理权限'),
(10004, 1, 'internal-admin', 10, 3, '清理操作日志', 'system:log:operation:delete', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:log:operation:delete', '操作日志清理权限')
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `app_code` = VALUES(`app_code`),
    `parent_id` = VALUES(`parent_id`),
    `menu_type` = VALUES(`menu_type`),
    `menu_name` = VALUES(`menu_name`),
    `menu_code` = VALUES(`menu_code`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `visible` = VALUES(`visible`),
    `keep_alive` = VALUES(`keep_alive`),
    `embedded` = VALUES(`embedded`),
    `redirect` = VALUES(`redirect`),
    `permissions` = VALUES(`permissions`),
    `remark` = VALUES(`remark`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT CAST(CONV(SUBSTRING(MD5(CONCAT('log-clean-role-menu:', `r`.`id`, ':', `m`.`id`)), 1, 15), 16, 10) AS UNSIGNED),
       `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (9004, 10004)
LEFT JOIN `authorization_role_menu` `existing`
  ON `existing`.`tenant_id` = `r`.`tenant_id`
 AND `existing`.`role_id` = `r`.`id`
 AND `existing`.`menu_id` = `m`.`id`
WHERE `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
  AND `existing`.`id` IS NULL
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
