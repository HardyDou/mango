CREATE TABLE IF NOT EXISTS `authorization_app_login_context` (
    `id` BIGINT NOT NULL COMMENT '主键' PRIMARY KEY,
    `app_id` BIGINT NOT NULL COMMENT '应用ID',
    `app_code` VARCHAR(64) NOT NULL COMMENT '应用编码',
    `realm` VARCHAR(32) NOT NULL COMMENT '登录域',
    `actor_type` VARCHAR(32) NOT NULL COMMENT '操作者类型',
    `default_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认上下文: 0-否, 1-是',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
    UNIQUE KEY `uk_authorization_app_context` (`app_code`, `realm`, `actor_type`),
    KEY `idx_authorization_app_context_app_id` (`app_id`),
    KEY `idx_authorization_app_context_realm` (`realm`),
    KEY `idx_authorization_app_context_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='授权应用登录上下文表';

INSERT INTO `authorization_app_login_context` (
    `id`, `app_id`, `app_code`, `realm`, `actor_type`, `default_flag`, `status`, `sort`
)
SELECT
    `id`,
    `id`,
    `app_code`,
    `realm`,
    COALESCE(NULLIF(`actor_type`, ''), CONCAT(`realm`, '_USER')),
    1,
    `status`,
    0
FROM `authorization_app`
WHERE `realm` IS NOT NULL AND `realm` <> ''
ON DUPLICATE KEY UPDATE
    `app_id` = VALUES(`app_id`),
    `default_flag` = VALUES(`default_flag`),
    `status` = VALUES(`status`),
    `sort` = VALUES(`sort`);

ALTER TABLE `authorization_app`
    MODIFY COLUMN `realm` VARCHAR(32) DEFAULT NULL COMMENT '已迁移至 authorization_app_login_context.realm',
    MODIFY COLUMN `actor_type` VARCHAR(32) DEFAULT NULL COMMENT '已迁移至 authorization_app_login_context.actor_type';
