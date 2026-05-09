INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(1702, 1, 'internal-admin', 17, 3, '新增组织', 'system:org:add', NULL, NULL, NULL, 2, 1, 0, 0, 0, NULL, 'system:org:add', '组织新增权限'),
(1703, 1, 'internal-admin', 17, 3, '修改组织', 'system:org:edit', NULL, NULL, NULL, 3, 1, 0, 0, 0, NULL, 'system:org:edit', '组织修改权限'),
(1704, 1, 'internal-admin', 17, 3, '删除组织', 'system:org:delete', NULL, NULL, NULL, 4, 1, 0, 0, 0, NULL, 'system:org:delete', '组织删除权限')
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
SELECT CAST(CONV(SUBSTRING(MD5(CONCAT('org-crud-role-menu:', `r`.`id`, ':', `m`.`id`)), 1, 15), 16, 10) AS UNSIGNED),
       `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (1702, 1703, 1704)
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
