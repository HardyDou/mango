CREATE TABLE IF NOT EXISTS `guarantee_module_marker` (
    `id` BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
    `module_name` VARCHAR(64) NOT NULL COMMENT '模块名称',
    `module_stage` VARCHAR(64) NOT NULL COMMENT '模块阶段',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id` VARCHAR(64) NOT NULL DEFAULT '1' COMMENT '机构标识',
    UNIQUE KEY `uk_guarantee_module_marker_name` (`module_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保函协同模块迁移标记表';

INSERT INTO `guarantee_module_marker` (
    `id`, `module_name`, `module_stage`, `tenant_id`
) VALUES (
    1, 'mango-guarantee', 'collaboration-foundation', '1'
) ON DUPLICATE KEY UPDATE
    `module_stage` = VALUES(`module_stage`),
    `updated_at` = CURRENT_TIMESTAMP;
