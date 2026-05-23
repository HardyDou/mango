UPDATE `authorization_menu`
SET `menu_name` = '编号规则',
    `remark` = '编号生成器、版本、片段和流水管理',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2710 OR `menu_code` = 'data:numgen';

UPDATE `authorization_menu`
SET `menu_name` = '编号规则查询',
    `remark` = '编号规则查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 271001 OR `menu_code` = 'numgen:manage:list';

UPDATE `authorization_menu`
SET `menu_name` = '编号规则维护',
    `remark` = '编号规则维护权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 271002 OR `menu_code` = 'numgen:manage:write';
