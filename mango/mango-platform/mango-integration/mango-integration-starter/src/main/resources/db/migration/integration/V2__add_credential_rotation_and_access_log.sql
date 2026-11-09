CREATE TABLE IF NOT EXISTS `integration_credential_rotation` (
    `id` bigint NOT NULL COMMENT '主键',
    `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
    `org_id` bigint DEFAULT NULL COMMENT '组织标识',
    `credential_code` varchar(64) NOT NULL COMMENT '凭证编码',
    `version` int NOT NULL COMMENT '轮换后版本',
    `field_codes` varchar(500) DEFAULT NULL COMMENT '本次更新的字段编码，逗号分隔',
    `operator_id` bigint DEFAULT NULL COMMENT '操作人',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `created_by` bigint DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` bigint DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_integration_credential_rotation_code` (`credential_code`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集成凭证轮换记录';

CREATE TABLE IF NOT EXISTS `integration_credential_access_log` (
    `id` bigint NOT NULL COMMENT '主键',
    `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
    `org_id` bigint DEFAULT NULL COMMENT '组织标识',
    `credential_code` varchar(64) NOT NULL COMMENT '凭证编码',
    `action` varchar(32) NOT NULL COMMENT '动作：READ',
    `field_codes` varchar(500) DEFAULT NULL COMMENT '读取的字段编码，逗号分隔',
    `operator_id` bigint DEFAULT NULL COMMENT '操作人',
    `source` varchar(64) DEFAULT NULL COMMENT '调用来源',
    `created_by` bigint DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` bigint DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_integration_credential_access_code` (`credential_code`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集成凭证读取记录';
