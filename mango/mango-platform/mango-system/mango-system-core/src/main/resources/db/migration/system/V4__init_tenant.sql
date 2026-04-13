CREATE TABLE IF NOT EXISTS `sys_tenant` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
    `tenant_name` VARCHAR(100) NOT NULL COMMENT '租户名称',
    `tenant_code` VARCHAR(50) NOT NULL COMMENT '租户编码' UNIQUE,
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `contact`     VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    `mobile`      VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

INSERT INTO `sys_tenant` (`id`, `tenant_name`, `tenant_code`, `status`) VALUES
(1, '默认租户', 'default', 1);
