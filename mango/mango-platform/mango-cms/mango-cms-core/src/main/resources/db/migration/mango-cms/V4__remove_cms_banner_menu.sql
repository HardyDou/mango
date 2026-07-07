SET @schema_name = DATABASE();

SET @remove_cms_banner_role_menu = (
  SELECT IF(COUNT(DISTINCT TABLE_NAME) = 2,
    'DELETE FROM `authorization_role_menu` WHERE `menu_id` IN (SELECT `id` FROM `authorization_menu` WHERE `module_code` = ''mango-cms'' AND (`menu_code` = ''cms:banner'' OR `menu_code` LIKE ''cms:banner:%''))',
    'SELECT 1')
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME IN ('authorization_role_menu', 'authorization_menu')
);
PREPARE stmt FROM @remove_cms_banner_role_menu;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @remove_cms_banner_package_item = (
  SELECT IF(COUNT(DISTINCT TABLE_NAME) = 2,
    'DELETE FROM `authorization_menu_package_item` WHERE `menu_id` IN (SELECT `id` FROM `authorization_menu` WHERE `module_code` = ''mango-cms'' AND (`menu_code` = ''cms:banner'' OR `menu_code` LIKE ''cms:banner:%''))',
    'SELECT 1')
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME IN ('authorization_menu_package_item', 'authorization_menu')
);
PREPARE stmt FROM @remove_cms_banner_package_item;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @disable_cms_banner_menu = (
  SELECT IF(COUNT(*) = 1,
    'UPDATE `authorization_menu` SET `status` = 0, `visible` = 0, `del_flag` = 1, `update_time` = CURRENT_TIMESTAMP, `updated_at` = CURRENT_TIMESTAMP WHERE `module_code` = ''mango-cms'' AND (`menu_code` = ''cms:banner'' OR `menu_code` LIKE ''cms:banner:%'')',
    'SELECT 1')
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'authorization_menu'
);
PREPARE stmt FROM @disable_cms_banner_menu;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @disable_cms_banner_resource = (
  SELECT IF(COUNT(DISTINCT TABLE_NAME) = 2,
    'UPDATE `resource_registry` SET `status` = ''INACTIVE'', `updated_at` = CURRENT_TIMESTAMP WHERE `module_code` = ''mango-cms'' AND `resource_type` = ''AUTH_MENU'' AND `target_table` = ''authorization_menu'' AND `target_id` IN (SELECT `id` FROM `authorization_menu` WHERE `module_code` = ''mango-cms'' AND (`menu_code` = ''cms:banner'' OR `menu_code` LIKE ''cms:banner:%''))',
    'SELECT 1')
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME IN ('resource_registry', 'authorization_menu')
);
PREPARE stmt FROM @disable_cms_banner_resource;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
