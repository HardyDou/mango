ALTER TABLE `sys_tenant`
    ADD COLUMN `institution_type` VARCHAR(32) NOT NULL DEFAULT 'PLATFORM'
        COMMENT '机构类型',
    ADD COLUMN `capability_codes` VARCHAR(500) DEFAULT NULL
        COMMENT '开通能力编码，多个用逗号分隔';

UPDATE `sys_tenant`
SET `institution_type` = CASE `tenant_code`
    WHEN 'default' THEN 'PLATFORM'
    ELSE 'ENTERPRISE'
END
WHERE `institution_type` IS NULL OR `institution_type` = '';

UPDATE `sys_tenant`
SET `capability_codes` = CASE `tenant_code`
    WHEN 'default' THEN 'PLATFORM_ADMIN,SYSTEM_ADMIN,AUTH_ADMIN,ORG_ADMIN,WORKFLOW'
    ELSE 'SYSTEM_ADMIN,AUTH_ADMIN,ORG_ADMIN,WORKFLOW'
END
WHERE `capability_codes` IS NULL OR `capability_codes` = '';

INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `status`, `remark`)
VALUES
    (30, 'institution_type', '机构类型', 1, '机构主体分类'),
    (31, 'institution_capability', '机构能力', 1, '机构空间开通能力')
ON DUPLICATE KEY UPDATE
    `dict_name` = VALUES(`dict_name`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`, `status`, `remark`)
VALUES
    (300, 'institution_type', '平台运营方', 'PLATFORM', 1, 1, '平台自身运营和代维主体'),
    (301, 'institution_type', '企业机构', 'ENTERPRISE', 2, 1, '普通企业机构'),
    (302, 'institution_type', '担保机构', 'GUARANTEE', 3, 1, '融资性或非融资性担保机构'),
    (303, 'institution_type', '金融机构', 'FINANCIAL', 4, 1, '银行等金融机构'),
    (304, 'institution_type', '客户企业', 'CUSTOMER_ENTERPRISE', 5, 1, '申请业务的客户企业'),
    (310, 'institution_capability', '平台管理', 'PLATFORM_ADMIN', 1, 1, '平台级运营管理'),
    (311, 'institution_capability', '系统管理', 'SYSTEM_ADMIN', 2, 1, '系统基础配置管理'),
    (312, 'institution_capability', '权限管理', 'AUTH_ADMIN', 3, 1, '应用、菜单、角色、成员权限管理'),
    (313, 'institution_capability', '组织管理', 'ORG_ADMIN', 4, 1, '组织、岗位、成员组织关系管理'),
    (314, 'institution_capability', '流程管理', 'WORKFLOW', 5, 1, '审批流程配置与执行'),
    (315, 'institution_capability', '保函业务', 'GUARANTEE_BUSINESS', 6, 1, '保函业务处理能力'),
    (316, 'institution_capability', '银行协同', 'BANK_COLLABORATION', 7, 1, '银行资料协同与业务处理')
ON DUPLICATE KEY UPDATE
    `dict_label` = VALUES(`dict_label`),
    `dict_value` = VALUES(`dict_value`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);
