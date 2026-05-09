CREATE TABLE IF NOT EXISTS `tenant_member` (
    `id` BIGINT NOT NULL COMMENT '成员ID' PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '全局账号ID',
    `member_no` VARCHAR(64) DEFAULT NULL COMMENT '成员编号',
    `display_name` VARCHAR(100) NOT NULL COMMENT '成员显示名称',
    `member_type` VARCHAR(32) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '成员类型',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `primary_org_id` BIGINT DEFAULT NULL COMMENT '主组织ID',
    `primary_post_id` BIGINT DEFAULT NULL COMMENT '主岗位ID',
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `left_at` DATETIME DEFAULT NULL COMMENT '离开时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_tenant_member_tenant_user` (`tenant_id`, `user_id`),
    UNIQUE KEY `uk_tenant_member_tenant_no` (`tenant_id`, `member_no`),
    KEY `idx_tenant_member_user` (`user_id`),
    KEY `idx_tenant_member_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户成员表';

CREATE TABLE IF NOT EXISTS `tenant_member_org` (
    `id` BIGINT NOT NULL COMMENT '主键' PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `member_id` BIGINT NOT NULL COMMENT '成员ID',
    `org_id` BIGINT NOT NULL COMMENT '组织ID',
    `post_id` BIGINT DEFAULT NULL COMMENT '岗位ID',
    `primary_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '是否主组织岗位: 0-否, 1-是',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_tenant_member_org_member_org_post` (`tenant_id`, `member_id`, `org_id`, `post_id`),
    KEY `idx_tenant_member_org_member` (`member_id`),
    KEY `idx_tenant_member_org_org` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户成员组织岗位关系表';

INSERT INTO `tenant_member`
    (`id`, `tenant_id`, `user_id`, `member_no`, `display_name`, `member_type`, `status`, `remark`)
SELECT 1000 + `t`.`id`,
       `t`.`id`,
       1,
       CONCAT('ADMIN-', `t`.`tenant_code`),
       CONCAT(`t`.`tenant_name`, '管理员'),
       'TENANT_ADMIN',
       1,
       CONCAT(`t`.`tenant_name`, '初始化管理员成员')
FROM `sys_tenant` `t`
WHERE `t`.`id` IN (1, 2, 3, 4)
ON DUPLICATE KEY UPDATE
    `display_name` = VALUES(`display_name`),
    `member_type` = VALUES(`member_type`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);
