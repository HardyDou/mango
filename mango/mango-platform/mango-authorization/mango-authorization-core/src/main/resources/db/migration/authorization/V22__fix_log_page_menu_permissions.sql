UPDATE `authorization_menu`
SET `menu_code` = 'system:log:login',
    `permissions` = 'system:log:login:list',
    `remark` = '登录日志页面入口'
WHERE `id` = 9
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `menu_code` = 'system:log:operation',
    `permissions` = 'system:log:operation:list',
    `remark` = '操作日志页面入口'
WHERE `id` = 10
  AND `app_code` = 'internal-admin';
