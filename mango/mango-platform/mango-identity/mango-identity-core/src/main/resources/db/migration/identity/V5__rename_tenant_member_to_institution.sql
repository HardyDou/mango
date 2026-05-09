UPDATE `tenant_member`
SET `member_type` = 'INSTITUTION_ADMIN'
WHERE `member_type` = 'TENANT_ADMIN';

UPDATE `tenant_member`
SET `remark` = REPLACE(REPLACE(REPLACE(`remark`, '租户创建者', '机构创建者'), '租户初始化管理员成员', '机构初始化管理员成员'), 'E2E租户', 'E2E机构')
WHERE `remark` LIKE '%租户%';
