UPDATE `authorization_menu`
SET `menu_name` = '发送任务',
    `remark` = '人工触发业务消息发送任务入口',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2947
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '发送任务查询',
    `remark` = '发送任务页面查询业务消息权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294701
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '发送任务提交',
    `remark` = '发送任务提交权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294702
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';
