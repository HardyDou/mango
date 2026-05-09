UPDATE `authorization_menu`
SET `menu_name` = '机构管理',
    `remark` = '机构管理'
WHERE `id` = 12
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `menu_name` = '查询机构',
    `remark` = '机构详情查询权限'
WHERE `id` = 1201
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `menu_name` = '新增机构',
    `remark` = '机构新增权限'
WHERE `id` = 1202
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `menu_name` = '编辑机构',
    `remark` = '机构编辑权限'
WHERE `id` = 1203
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `menu_name` = '删除机构',
    `remark` = '机构删除权限'
WHERE `id` = 1204
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `remark` = '平台级机构、应用和系统元数据管理'
WHERE `id` = 19
  AND `app_code` = 'internal-admin';

UPDATE `authorization_menu`
SET `icon` = CASE
    WHEN `menu_code` = 'system' THEN 'Setting'
    WHEN `menu_code` = 'system:account-access' THEN 'UserFilled'
    WHEN `menu_code` = 'system:org-hr' THEN 'Connection'
    WHEN `menu_code` = 'system:platform-ops' THEN 'Platform'
    WHEN `menu_code` = 'system:base-data' THEN 'DataBoard'
    WHEN `menu_code` = 'system:audit-log' THEN 'DocumentChecked'
    WHEN `menu_code` = 'system:user' THEN 'User'
    WHEN `menu_code` = 'system:role' THEN 'UserFilled'
    WHEN `menu_code` = 'system:dict' THEN 'Collection'
    WHEN `menu_code` = 'system:config' THEN 'Setting'
    WHEN `menu_code` = 'system:area' THEN 'MapLocation'
    WHEN `menu_code` = 'system:route' THEN 'Switch'
    WHEN `menu_code` = 'system:log:login' THEN 'DocumentChecked'
    WHEN `menu_code` = 'system:log:operation' THEN 'Tickets'
    WHEN `menu_code` = 'system:org' THEN 'OfficeBuilding'
    WHEN `menu_code` = 'system:post' THEN 'Postcard'
    WHEN `menu_code` = 'system:tenant' THEN 'OfficeBuilding'
    WHEN `menu_code` = 'system:app' THEN 'Box'
    WHEN `menu_code` = 'system:menu' THEN 'Menu'
    ELSE 'Menu'
END
WHERE `app_code` = 'internal-admin'
  AND `menu_type` <> 3
  AND `visible` = 1
  AND `status` = 1
  AND (`icon` IS NULL OR `icon` = '');
