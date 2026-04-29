-- ================================================
-- V1: 身份模块初始化表结构
-- ================================================

CREATE TABLE IF NOT EXISTS `identity_user` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID' PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码哈希',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称',
    `realm` VARCHAR(32) NOT NULL DEFAULT 'INTERNAL' COMMENT '登录域',
    `actor_type` VARCHAR(32) NOT NULL DEFAULT 'INTERNAL_USER' COMMENT '操作者类型',
    `party_type` VARCHAR(64) DEFAULT NULL COMMENT '归属主体类型',
    `party_id` BIGINT DEFAULT NULL COMMENT '归属主体ID',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最近登录时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    UNIQUE KEY `uk_identity_user_realm_username` (`realm`, `username`),
    KEY `idx_identity_user_username` (`username`),
    KEY `idx_identity_user_party` (`party_type`, `party_id`),
    KEY `idx_identity_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身份用户表';
