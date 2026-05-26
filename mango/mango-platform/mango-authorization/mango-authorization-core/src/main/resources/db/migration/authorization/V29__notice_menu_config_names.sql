UPDATE `authorization_menu`
SET `menu_name` = '渠道配置',
    `remark` = '系统可用通知通道账号和结构化接入配置',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2942
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '消息配置',
    `remark` = '右上角小铃铛用户消息入口和站内消息配置入口',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2946
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '渠道配置查询',
    `remark` = '渠道配置查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294201
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '渠道配置创建',
    `remark` = '渠道配置创建权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294202
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '渠道配置编辑',
    `remark` = '渠道配置编辑权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294203
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';

UPDATE `authorization_menu`
SET `menu_name` = '消息配置查询',
    `remark` = '消息配置查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 294601
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-notice';
