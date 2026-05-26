CREATE TABLE IF NOT EXISTS `notice_business_type` (
  `id` bigint NOT NULL COMMENT '主键',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型编码',
  `biz_name` varchar(128) NOT NULL COMMENT '业务类型名称',
  `biz_group` varchar(64) DEFAULT NULL COMMENT '业务分组',
  `description` varchar(500) DEFAULT NULL COMMENT '说明',
  `params_schema` text DEFAULT NULL COMMENT '参数 schema JSON',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `default_priority` varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT '默认优先级',
  `idempotent_strategy` varchar(64) DEFAULT NULL COMMENT '幂等策略',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_biz_type` (`tenant_id`, `biz_type`),
  KEY `idx_notice_biz_group` (`tenant_id`, `biz_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知业务类型表';

CREATE TABLE IF NOT EXISTS `notice_business_channel_template` (
  `id` bigint NOT NULL COMMENT '主键',
  `business_type_id` bigint NOT NULL COMMENT '业务类型 ID',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型编码快照',
  `channel_type` varchar(32) NOT NULL COMMENT '渠道类型',
  `template_name` varchar(128) DEFAULT NULL COMMENT '模板名称',
  `title_template` varchar(200) DEFAULT NULL COMMENT '标题模板',
  `content_template` text DEFAULT NULL COMMENT '内容模板',
  `channel_template_id` varchar(128) DEFAULT NULL COMMENT '三方模板 ID',
  `variable_mapping` text DEFAULT NULL COMMENT '变量映射 JSON',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `version_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '版本状态',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `publish_by` bigint DEFAULT NULL COMMENT '发布人 ID',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_biz_channel_version` (`tenant_id`, `biz_type`, `channel_type`, `version`),
  KEY `idx_notice_biz_channel_active` (`tenant_id`, `biz_type`, `channel_type`, `version_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知业务渠道模板表';

CREATE TABLE IF NOT EXISTS `notice_channel_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `channel_type` varchar(32) NOT NULL COMMENT '渠道类型',
  `provider_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `config_name` varchar(128) DEFAULT NULL COMMENT '配置名称',
  `config_json` text DEFAULT NULL COMMENT '配置 JSON 或密文引用',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `priority` int NOT NULL DEFAULT 0 COMMENT '优先级',
  `rate_limit_config` text DEFAULT NULL COMMENT '频控配置 JSON',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_channel_type` (`tenant_id`, `channel_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知渠道配置表';

CREATE TABLE IF NOT EXISTS `notice_task` (
  `id` bigint NOT NULL COMMENT '主键',
  `task_code` varchar(64) NOT NULL COMMENT '任务编码',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `biz_id` varchar(128) DEFAULT NULL COMMENT '业务对象 ID',
  `idempotent_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `params_snapshot` text DEFAULT NULL COMMENT '业务参数快照 JSON',
  `channel_types` varchar(255) DEFAULT NULL COMMENT '实际渠道集合快照',
  `send_mode` varchar(32) NOT NULL DEFAULT 'IMMEDIATE' COMMENT '发送模式',
  `scheduled_time` datetime DEFAULT NULL COMMENT '定时发送时间',
  `status` varchar(32) NOT NULL DEFAULT 'WAITING' COMMENT '任务状态',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总发送数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_task_code` (`task_code`),
  UNIQUE KEY `uk_notice_task_idempotent` (`tenant_id`, `idempotent_key`),
  KEY `idx_notice_task_biz` (`tenant_id`, `biz_type`, `biz_id`),
  KEY `idx_notice_task_status` (`tenant_id`, `status`, `scheduled_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知任务表';

CREATE TABLE IF NOT EXISTS `notice_recipient` (
  `id` bigint NOT NULL COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务 ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户 ID',
  `recipient_name` varchar(128) DEFAULT NULL COMMENT '接收人名称快照',
  `mobile` varchar(32) DEFAULT NULL COMMENT '手机号快照',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱快照',
  `wechat_openid` varchar(128) DEFAULT NULL COMMENT '微信 openid 快照',
  `wecom_user_id` varchar(128) DEFAULT NULL COMMENT '企业微信 userId 快照',
  `dingtalk_user_id` varchar(128) DEFAULT NULL COMMENT '钉钉 userId 快照',
  `external_id` varchar(128) DEFAULT NULL COMMENT '外部联系人标识',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_recipient_task` (`task_id`),
  KEY `idx_notice_recipient_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知接收人快照表';

CREATE TABLE IF NOT EXISTS `notice_send_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务 ID',
  `recipient_id` bigint NOT NULL COMMENT '接收人 ID',
  `business_channel_template_id` bigint DEFAULT NULL COMMENT '业务渠道模板 ID',
  `template_version` int DEFAULT NULL COMMENT '模板版本',
  `channel_type` varchar(32) NOT NULL COMMENT '渠道类型',
  `channel_config_id` bigint DEFAULT NULL COMMENT '渠道配置 ID',
  `request_id` varchar(128) NOT NULL COMMENT '请求流水号',
  `provider_message_id` varchar(128) DEFAULT NULL COMMENT '供应商消息 ID',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态',
  `rendered_title` varchar(200) DEFAULT NULL COMMENT '渲染后标题',
  `rendered_content` text DEFAULT NULL COMMENT '渲染后内容快照',
  `request_snapshot` text DEFAULT NULL COMMENT '请求摘要 JSON',
  `response_snapshot` text DEFAULT NULL COMMENT '响应摘要 JSON',
  `fail_code` varchar(64) DEFAULT NULL COMMENT '失败码',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `sent_at` datetime DEFAULT NULL COMMENT '发送时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_send_request` (`tenant_id`, `request_id`),
  KEY `idx_notice_send_task` (`task_id`),
  KEY `idx_notice_send_status` (`tenant_id`, `status`, `next_retry_time`),
  KEY `idx_notice_send_recipient` (`tenant_id`, `recipient_id`, `channel_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知发送记录表';

CREATE TABLE IF NOT EXISTS `notice_site_message` (
  `id` bigint NOT NULL COMMENT '主键',
  `task_id` bigint DEFAULT NULL COMMENT '来源任务 ID',
  `send_record_id` bigint DEFAULT NULL COMMENT '来源发送记录 ID',
  `user_id` bigint NOT NULL COMMENT '接收用户 ID',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `priority` varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT '优先级',
  `read_status` varchar(32) NOT NULL DEFAULT 'UNREAD' COMMENT '已读状态',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `delete_status` varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT '删除状态',
  `revoke_status` tinyint NOT NULL DEFAULT 0 COMMENT '是否撤回',
  `top_status` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `biz_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `biz_id` varchar(128) DEFAULT NULL COMMENT '业务对象 ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  PRIMARY KEY (`id`),
  KEY `idx_notice_site_user` (`tenant_id`, `user_id`, `delete_status`, `created_at`),
  KEY `idx_notice_site_unread` (`tenant_id`, `user_id`, `read_status`),
  KEY `idx_notice_site_biz` (`tenant_id`, `biz_type`, `biz_id`),
  KEY `idx_notice_site_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内信消息表';

CREATE TABLE IF NOT EXISTS `notice_retry_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `send_record_id` bigint NOT NULL COMMENT '发送记录 ID',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `retry_type` varchar(32) NOT NULL COMMENT '重试类型',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  PRIMARY KEY (`id`),
  KEY `idx_notice_retry_record` (`send_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知重试日志表';

CREATE TABLE IF NOT EXISTS `notice_callback_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `channel_type` varchar(32) NOT NULL COMMENT '渠道类型',
  `provider_message_id` varchar(128) DEFAULT NULL COMMENT '供应商消息 ID',
  `callback_snapshot` text DEFAULT NULL COMMENT '回调摘要',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  PRIMARY KEY (`id`),
  KEY `idx_notice_callback_provider` (`tenant_id`, `provider_message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知回调日志表';

CREATE TABLE IF NOT EXISTS `notice_setting` (
  `id` bigint NOT NULL COMMENT '主键',
  `setting_key` varchar(128) NOT NULL COMMENT '配置键',
  `setting_value` text DEFAULT NULL COMMENT '配置值',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_setting_key` (`tenant_id`, `setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知设置表';

CREATE TABLE IF NOT EXISTS `notice_audit_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `action_type` varchar(64) NOT NULL COMMENT '操作类型',
  `target_type` varchar(64) NOT NULL COMMENT '目标类型',
  `target_id` bigint DEFAULT NULL COMMENT '目标 ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `audit_snapshot` text DEFAULT NULL COMMENT '审计摘要',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_audit_target` (`tenant_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知审计日志表';
