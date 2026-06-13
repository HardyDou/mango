ALTER TABLE `payment_application`
  ADD COLUMN `secret_configured` tinyint NOT NULL DEFAULT '0' COMMENT '应用密钥是否已配置',
  ADD COLUMN `secret_version` int NOT NULL DEFAULT '0' COMMENT '应用密钥版本',
  ADD COLUMN `secret_last_reset_time` datetime DEFAULT NULL COMMENT '应用密钥最后重置时间',
  ADD COLUMN `timestamp_tolerance_seconds` int NOT NULL DEFAULT '300' COMMENT '请求时间戳有效期秒数',
  ADD COLUMN `nonce_replay_window_seconds` int NOT NULL DEFAULT '300' COMMENT 'Nonce 防重放窗口秒数',
  ADD COLUMN `refund_notify_url` varchar(512) DEFAULT NULL COMMENT '退款结果通知地址',
  ADD COLUMN `notify_retry_policy` varchar(512) DEFAULT NULL COMMENT '通知重试策略',
  ADD COLUMN `enabled_subject_ids` varchar(1024) DEFAULT NULL COMMENT '可用企业主体 ID，逗号分隔',
  ADD COLUMN `enabled_method_codes` varchar(1024) DEFAULT NULL COMMENT '可用标准支付方式编码，逗号分隔',
  ADD COLUMN `refund_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许退款',
  ADD COLUMN `partial_refund_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许部分退款',
  ADD COLUMN `demo_app` tinyint NOT NULL DEFAULT '0' COMMENT '是否示例应用';
