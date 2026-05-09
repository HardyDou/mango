UPDATE `authorization_role`
SET `role_name` = '机构管理员',
    `remark` = REPLACE(REPLACE(`remark`, '租户管理员', '机构管理员'), '默认管理员角色', '默认机构管理员角色')
WHERE `role_name` = '租户管理员'
   OR `remark` LIKE '%租户管理员%'
   OR `remark` LIKE '%默认管理员角色%';

UPDATE `authorization_role`
SET `remark` = REPLACE(`remark`, 'E2E租户', 'E2E机构')
WHERE `remark` LIKE '%E2E租户%';
