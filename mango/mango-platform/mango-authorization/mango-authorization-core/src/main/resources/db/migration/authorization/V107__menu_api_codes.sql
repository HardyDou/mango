SET @menu_api_codes_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'authorization_menu'
    AND COLUMN_NAME = 'api_codes'
);
SET @add_menu_api_codes_column_sql := IF(
  @menu_api_codes_column_exists = 0,
  'ALTER TABLE `authorization_menu` ADD COLUMN `api_codes` varchar(2000) DEFAULT NULL COMMENT ''菜单携带的接口/动作权限码列表'' AFTER `permissions`',
  'SELECT 1'
);
PREPARE add_menu_api_codes_column_stmt FROM @add_menu_api_codes_column_sql;
EXECUTE add_menu_api_codes_column_stmt;
DEALLOCATE PREPARE add_menu_api_codes_column_stmt;

UPDATE `authorization_menu`
SET `api_codes` = `permissions`
WHERE (`api_codes` IS NULL OR `api_codes` = '')
  AND `permissions` IS NOT NULL
  AND `permissions` <> ''
  AND `menu_type` IN (1, 2);

SET SESSION group_concat_max_len = 65535;

UPDATE `authorization_menu` parent
JOIN (
  SELECT
    `parent_id`,
    GROUP_CONCAT(DISTINCT COALESCE(NULLIF(`permissions`, ''), NULLIF(`menu_code`, '')) ORDER BY `sort`, `id` SEPARATOR ',') AS `codes`
  FROM `authorization_menu`
  WHERE `menu_type` = 3
    AND `parent_id` IS NOT NULL
    AND `parent_id` > 0
    AND COALESCE(NULLIF(`permissions`, ''), NULLIF(`menu_code`, '')) IS NOT NULL
  GROUP BY `parent_id`
) child_codes ON child_codes.`parent_id` = parent.`id`
SET parent.`api_codes` = CASE
  WHEN parent.`api_codes` IS NULL OR parent.`api_codes` = '' THEN child_codes.`codes`
  ELSE CONCAT(parent.`api_codes`, ',', child_codes.`codes`)
END
WHERE parent.`menu_type` IN (1, 2);

DELETE rm
FROM `authorization_role_menu` rm
JOIN `authorization_menu` menu ON menu.`id` = rm.`menu_id`
WHERE menu.`menu_type` = 3;

DELETE mpi
FROM `authorization_menu_package_item` mpi
JOIN `authorization_menu` menu ON menu.`id` = mpi.`menu_id`
WHERE menu.`menu_type` = 3;

DELETE runtime_config
FROM `frontend_menu_runtime_config` runtime_config
JOIN `authorization_menu` menu ON menu.`id` = runtime_config.`menu_id`
WHERE menu.`menu_type` = 3;

DELETE FROM `authorization_menu`
WHERE `menu_type` = 3;

INSERT INTO `authorization_role`
(`id`, `tenant_id`, `app_code`, `realm`, `actor_type`, `role_code`, `role_name`, `role_type`, `status`, `sort`, `remark`)
SELECT UUID_SHORT(), seed.`tenant_id`, seed.`app_code`, seed.`realm`, seed.`actor_type`,
       'ROLE_ANONYMOUS', '匿名默认角色', 1, 1, -20, '平台默认角色：匿名基础权限'
FROM (
  SELECT `tenant_id`, `app_code`, MIN(`realm`) AS `realm`, MIN(`actor_type`) AS `actor_type`
  FROM `authorization_role`
  GROUP BY `tenant_id`, `app_code`
  UNION
  SELECT 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM `authorization_role` existing
  WHERE existing.`tenant_id` = seed.`tenant_id`
    AND existing.`app_code` = seed.`app_code`
    AND existing.`role_code` = 'ROLE_ANONYMOUS'
);

INSERT INTO `authorization_role`
(`id`, `tenant_id`, `app_code`, `realm`, `actor_type`, `role_code`, `role_name`, `role_type`, `status`, `sort`, `remark`)
SELECT UUID_SHORT(), seed.`tenant_id`, seed.`app_code`, seed.`realm`, seed.`actor_type`,
       'ROLE_LOGIN', '登录默认角色', 1, 1, -10, '平台默认角色：登录基础权限'
FROM (
  SELECT `tenant_id`, `app_code`, MIN(`realm`) AS `realm`, MIN(`actor_type`) AS `actor_type`
  FROM `authorization_role`
  GROUP BY `tenant_id`, `app_code`
  UNION
  SELECT 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM `authorization_role` existing
  WHERE existing.`tenant_id` = seed.`tenant_id`
    AND existing.`app_code` = seed.`app_code`
    AND existing.`role_code` = 'ROLE_LOGIN'
);
