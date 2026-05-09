UPDATE `authorization_menu`
SET `menu_name` = '成员管理',
    `remark` = '机构成员账号管理'
WHERE `app_code` = 'internal-admin'
  AND `menu_code` = 'system:user';

UPDATE `authorization_menu`
SET `menu_name` = '查询成员',
    `remark` = '机构成员查询权限'
WHERE `app_code` = 'internal-admin'
  AND `permissions` = 'system:user:list';

UPDATE `authorization_menu`
SET `menu_name` = '新增成员',
    `remark` = '机构成员新增权限'
WHERE `app_code` = 'internal-admin'
  AND `permissions` = 'system:user:add';

UPDATE `authorization_menu`
SET `menu_name` = '编辑成员',
    `remark` = '机构成员编辑权限'
WHERE `app_code` = 'internal-admin'
  AND `permissions` = 'system:user:edit';

UPDATE `authorization_menu`
SET `menu_name` = '移除成员',
    `remark` = '机构成员移除权限'
WHERE `app_code` = 'internal-admin'
  AND `permissions` = 'system:user:delete';

UPDATE `authorization_menu`
SET `menu_name` = '重置成员密码',
    `remark` = '机构成员密码重置权限'
WHERE `app_code` = 'internal-admin'
  AND `permissions` = 'system:user:reset-password';

UPDATE `authorization_menu`
SET `menu_name` = '调整成员状态',
    `remark` = '机构成员状态调整权限'
WHERE `app_code` = 'internal-admin'
  AND `permissions` = 'system:user:status';

UPDATE `authorization_menu`
SET `menu_name` = '分配成员角色',
    `remark` = '机构成员角色分配权限'
WHERE `app_code` = 'internal-admin'
  AND `permissions` = 'authorization:role:assign'
  AND `menu_code` = 'system:user:assign-role';
