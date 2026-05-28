CREATE TABLE IF NOT EXISTS `notice_recipient_account` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `account_type` varchar(32) NOT NULL COMMENT '账户类型',
  `account_value` varchar(255) NOT NULL COMMENT '账户标识',
  `display_name` varchar(128) DEFAULT NULL COMMENT '显示名称',
  `verified_status` varchar(32) NOT NULL DEFAULT 'PENDING_VERIFY' COMMENT '验证状态',
  `default_account` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_recipient_account_value` (`tenant_id`, `user_id`, `account_type`, `account_value`),
  KEY `idx_notice_recipient_account_user` (`tenant_id`, `user_id`, `account_type`, `verified_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知接收账户表';

INSERT INTO `notice_recipient_account`
(`id`, `user_id`, `account_type`, `account_value`, `display_name`, `verified_status`, `default_account`, `enabled`,
 `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2060000000000004001, 1, 'EMAIL', '1012404303@qq.com', 'admin邮箱', 'VERIFIED', 1, 1,
 '1', 1, NOW(), 1, NOW()),
(2060000000000004002, 1, 'MOBILE', '18701445644', 'admin手机号', 'VERIFIED', 1, 1,
 '1', 1, NOW(), 1, NOW())
ON DUPLICATE KEY UPDATE
  `display_name` = VALUES(`display_name`),
  `verified_status` = VALUES(`verified_status`),
  `default_account` = VALUES(`default_account`),
  `enabled` = VALUES(`enabled`),
  `updated_by` = VALUES(`updated_by`),
  `updated_at` = VALUES(`updated_at`);

CREATE TABLE IF NOT EXISTS `notice_receive_preference` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `scope_type` varchar(32) NOT NULL COMMENT '范围类型',
  `scope_value` varchar(128) NOT NULL DEFAULT '' COMMENT '范围值',
  `channel_type` varchar(32) DEFAULT NULL COMMENT '渠道类型，空表示总开关',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否接收',
  `account_id` bigint DEFAULT NULL COMMENT '指定接收账户 ID',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_receive_preference` (`tenant_id`, `user_id`, `scope_type`, `scope_value`, `channel_type`),
  KEY `idx_notice_receive_preference_user` (`tenant_id`, `user_id`, `scope_type`, `scope_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知接收偏好表';
