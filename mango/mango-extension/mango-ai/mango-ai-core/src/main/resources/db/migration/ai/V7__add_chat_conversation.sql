CREATE TABLE IF NOT EXISTS `ai_chat_conversation` (
  `id` bigint NOT NULL,
  `session_id` varchar(128) NOT NULL,
  `user_id` bigint NOT NULL,
  `service_code` varchar(64) NOT NULL,
  `title` varchar(160) NOT NULL,
  `model_id` bigint NOT NULL,
  `model_name` varchar(128) NOT NULL,
  `provider_code` varchar(64) NOT NULL,
  `thinking_enabled` tinyint NOT NULL DEFAULT 0,
  `message_count` int NOT NULL DEFAULT 0,
  `tenant_id` varchar(64) NOT NULL,
  `org_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chat_conversation_session` (`tenant_id`, `user_id`, `service_code`, `session_id`),
  KEY `idx_ai_chat_conversation_user_time` (`tenant_id`, `user_id`, `service_code`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户 AI 服务对话';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` bigint NOT NULL,
  `conversation_id` bigint NOT NULL,
  `sequence_no` int NOT NULL,
  `role` varchar(16) NOT NULL,
  `content` text NOT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `org_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chat_message_sequence` (`tenant_id`, `conversation_id`, `sequence_no`),
  KEY `idx_ai_chat_message_conversation` (`tenant_id`, `conversation_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户 AI 服务对话消息';
