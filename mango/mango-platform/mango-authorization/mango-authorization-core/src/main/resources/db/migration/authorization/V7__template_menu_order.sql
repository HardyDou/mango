UPDATE `authorization_menu`
SET `sort` = 5,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 29
  AND `menu_code` = 'template';
