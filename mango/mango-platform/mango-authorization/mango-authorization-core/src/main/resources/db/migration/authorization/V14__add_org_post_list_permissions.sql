INSERT INTO `authorization_menu` (
    `id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`,
    `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`,
    `keep_alive`, `embedded`, `redirect`, `permissions`, `remark`
) VALUES
(1500, 1, 'internal-admin', 15, 3, '查询岗位列表', 'system:post:list', NULL, NULL, NULL, 0, 1, 0, 0, 0, NULL, 'system:post:list', '岗位列表查询权限'),
(1700, 1, 'internal-admin', 17, 3, '查询组织列表', 'system:org:list', NULL, NULL, NULL, 0, 1, 0, 0, 0, NULL, 'system:org:list', '组织列表查询权限')
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
SELECT 1000000 + `m`.`id`, `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (1500, 1700)
WHERE `r`.`tenant_id` = 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`)
SELECT (`r`.`tenant_id` * 100000) + `m`.`id`, `r`.`tenant_id`, `r`.`id`, `m`.`id`
FROM `authorization_role` `r`
JOIN `authorization_menu` `m` ON `m`.`id` IN (1500, 1700)
WHERE `r`.`tenant_id` <> 1
  AND `r`.`app_code` = 'internal-admin'
  AND `r`.`role_code` = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `role_id` = VALUES(`role_id`),
    `menu_id` = VALUES(`menu_id`);
