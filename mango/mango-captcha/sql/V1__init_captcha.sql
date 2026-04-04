-- 验证码表
CREATE TABLE IF NOT EXISTS `captcha_code` (
    `id` BIGINT(20) NOT NULL COMMENT '主键',
    `code_key` VARCHAR(64) NOT NULL COMMENT '验证码KEY',
    `code_value` VARCHAR(128) NOT NULL COMMENT '验证码值',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code_key` (`code_key`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码表';
