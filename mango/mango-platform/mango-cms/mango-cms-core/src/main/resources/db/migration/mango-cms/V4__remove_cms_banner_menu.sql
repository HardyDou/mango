DELETE FROM `authorization_role_menu`
WHERE `menu_id` IN (
  SELECT `id`
  FROM `authorization_menu`
  WHERE `module_code` = 'mango-cms'
    AND (`menu_code` = 'cms:banner' OR `menu_code` LIKE 'cms:banner:%')
);

DELETE FROM `authorization_menu_package_item`
WHERE `menu_id` IN (
  SELECT `id`
  FROM `authorization_menu`
  WHERE `module_code` = 'mango-cms'
    AND (`menu_code` = 'cms:banner' OR `menu_code` LIKE 'cms:banner:%')
);

UPDATE `authorization_menu`
SET `status` = 0,
    `visible` = 0,
    `del_flag` = 1,
    `update_time` = CURRENT_TIMESTAMP,
    `updated_at` = CURRENT_TIMESTAMP
WHERE `module_code` = 'mango-cms'
  AND (`menu_code` = 'cms:banner' OR `menu_code` LIKE 'cms:banner:%');

UPDATE `resource_registry`
SET `status` = 'INACTIVE',
    `updated_at` = CURRENT_TIMESTAMP
WHERE `module_code` = 'mango-cms'
  AND `resource_type` = 'AUTH_MENU'
  AND `target_table` = 'authorization_menu'
  AND `target_id` IN (
    SELECT `id`
    FROM `authorization_menu`
    WHERE `module_code` = 'mango-cms'
      AND (`menu_code` = 'cms:banner' OR `menu_code` LIKE 'cms:banner:%')
  );
