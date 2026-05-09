UPDATE `org_post`
SET `post_name` = '机构管理员',
    `post_code` = REPLACE(`post_code`, '_TENANT_ADMIN', '_INSTITUTION_ADMIN'),
    `remark` = REPLACE(REPLACE(`remark`, '租户默认管理员岗位', '机构默认管理员岗位'), '租户默认', '机构默认')
WHERE `post_name` = '租户管理员'
   OR `post_code` LIKE '%\_TENANT\_ADMIN'
   OR `remark` LIKE '%租户默认%';

UPDATE `org_post`
SET `remark` = REPLACE(`remark`, '租户默认', '机构默认')
WHERE `remark` LIKE '%租户默认%';
