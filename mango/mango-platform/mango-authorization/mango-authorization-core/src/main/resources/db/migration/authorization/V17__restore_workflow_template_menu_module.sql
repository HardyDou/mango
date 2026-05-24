UPDATE `authorization_menu`
SET `module_code` = 'mango-workflow',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `app_code` = 'internal-admin'
  AND `menu_code` LIKE 'workflow:template%';
