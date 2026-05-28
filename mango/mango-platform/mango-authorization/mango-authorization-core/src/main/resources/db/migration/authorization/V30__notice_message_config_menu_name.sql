UPDATE `authorization_menu`
SET `menu_name` = '消息配置',
    `remark` = '业务消息、参数、渠道启停和模板配置',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2941
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '消息配置查询',
    `remark` = '消息配置查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294101
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '消息配置创建',
    `remark` = '消息配置创建权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294102
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '消息配置编辑',
    `remark` = '消息配置编辑权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294103
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '消息配置发布',
    `remark` = '消息配置发布权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294104
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '消息配置启停',
    `remark` = '消息配置启停权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294105
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '站内消息',
    `remark` = '右上角小铃铛用户消息入口和站内消息配置入口',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2946
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '站内消息查询',
    `remark` = '站内消息查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294601
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';
