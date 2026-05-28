UPDATE `authorization_menu`
SET `visible` = 0,
    `sort` = 6,
    `remark` = '接收设置仅从右上角我的消息弹窗进入，不作为通知中心后台菜单展示',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2943;

UPDATE `authorization_menu`
SET `sort` = 3,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2944;

UPDATE `authorization_menu`
SET `sort` = 4,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2945;
