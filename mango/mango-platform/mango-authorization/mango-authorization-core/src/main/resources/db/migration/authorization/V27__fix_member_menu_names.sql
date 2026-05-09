UPDATE `authorization_menu`
SET `menu_name` = '成员管理',
    `remark` = '机构成员账号管理'
WHERE `app_code` = 'internal-admin'
  AND `menu_code` = 'system:user';

UPDATE `authorization_menu`
SET `menu_name` = '查询成员',
    `remark` = '机构成员详情查询权限'
WHERE `app_code` = 'internal-admin'
  AND `menu_code` = 'system:user:query';
