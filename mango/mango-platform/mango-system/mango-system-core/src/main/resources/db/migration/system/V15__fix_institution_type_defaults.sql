UPDATE `sys_tenant`
SET `institution_type` = 'PLATFORM'
WHERE `tenant_code` = 'default';

UPDATE `sys_tenant`
SET `institution_type` = 'ENTERPRISE'
WHERE `tenant_code` <> 'default'
  AND (`institution_type` IS NULL OR `institution_type` = '' OR `institution_type` = 'PLATFORM');

ALTER TABLE `sys_tenant`
    MODIFY COLUMN `institution_type` VARCHAR(32) NOT NULL DEFAULT 'ENTERPRISE'
        COMMENT '机构类型';
