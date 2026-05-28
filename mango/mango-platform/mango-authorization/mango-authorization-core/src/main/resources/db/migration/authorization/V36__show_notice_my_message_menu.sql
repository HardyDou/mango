UPDATE `authorization_menu`
SET `redirect` = '/notice/site-message',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2940
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '我的消息',
    `sort` = 1,
    `status` = 1,
    `visible` = 1,
    `remark` = '当前用户系统消息列表和接收设置入口',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2946
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '我的消息查询',
    `remark` = '我的消息查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294601
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 2,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2941
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 3,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2947
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 4,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2942
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 5,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2944
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 6,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2945
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `sort` = 7,
    `visible` = 0,
    `status` = 1,
    `remark` = '接收设置仅从我的消息页面设置按钮进入，不作为通知中心后台菜单展示',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2943
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';
