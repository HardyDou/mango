DELETE FROM `authorization_menu_package_item`
WHERE `menu_id` IN (2955, 295501);

DELETE FROM `authorization_role_menu`
WHERE `menu_id` IN (2955, 295501);

UPDATE `authorization_menu`
SET `status` = 0,
    `visible` = 0,
    `del_flag` = 1,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (2955, 295501)
   OR `menu_code` IN ('job:handler', 'job:handler:list');

UPDATE `authorization_menu`
SET `sort` = 5,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2956
  AND `menu_code` = 'job:engine';
