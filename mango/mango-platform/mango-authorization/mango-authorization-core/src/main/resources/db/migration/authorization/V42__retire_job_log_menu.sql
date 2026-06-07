DELETE FROM `authorization_menu_package_item`
WHERE `menu_id` = 2953;

DELETE FROM `authorization_role_menu`
WHERE `menu_id` = 2953;

UPDATE `authorization_menu`
SET `status` = 0,
    `visible` = 0,
    `del_flag` = 1,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2953
   OR `menu_code` = 'job:log';

UPDATE `authorization_menu`
SET `parent_id` = 2952,
    `sort` = 2,
    `status` = 1,
    `visible` = 0,
    `del_flag` = 0,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 295301
   OR `menu_code` = 'job:log:list';

UPDATE `authorization_menu`
SET `sort` = 3,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2954
  AND `menu_code` = 'job:worker';

UPDATE `authorization_menu`
SET `sort` = 4,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2956
  AND `menu_code` = 'job:engine';
