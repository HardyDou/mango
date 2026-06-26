CREATE TABLE IF NOT EXISTS `notice_announcement` (
  `id` bigint NOT NULL COMMENT '主键',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '公告状态：DRAFT、PUBLISHED、OFFLINE',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `valid_start_time` datetime DEFAULT NULL COMMENT '有效开始时间',
  `valid_end_time` datetime DEFAULT NULL COMMENT '有效结束时间',
  `pinned` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `confirm_required` tinyint NOT NULL DEFAULT 0 COMMENT '是否需要确认',
  `sync_message_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否同步系统消息提醒',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_announcement_status` (`tenant_id`, `status`, `publish_time`),
  KEY `idx_notice_announcement_valid` (`tenant_id`, `valid_start_time`, `valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';

CREATE TABLE IF NOT EXISTS `notice_announcement_target` (
  `id` bigint NOT NULL COMMENT '主键',
  `announcement_id` bigint NOT NULL COMMENT '公告 ID',
  `target_type` varchar(32) NOT NULL COMMENT '发布对象类型：ALL、ORG、ROLE、USER',
  `target_id` bigint DEFAULT NULL COMMENT '发布对象 ID',
  `target_name` varchar(128) DEFAULT NULL COMMENT '发布对象名称快照',
  `include_children` tinyint NOT NULL DEFAULT 0 COMMENT '组织是否包含下级',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_announcement_target` (`tenant_id`, `announcement_id`),
  KEY `idx_notice_announcement_target_value` (`tenant_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告发布对象快照表';

CREATE TABLE IF NOT EXISTS `notice_announcement_recipient` (
  `id` bigint NOT NULL COMMENT '主键',
  `announcement_id` bigint NOT NULL COMMENT '公告 ID',
  `user_id` bigint NOT NULL COMMENT '接收用户 ID',
  `read_status` varchar(32) NOT NULL DEFAULT 'UNREAD' COMMENT '阅读状态',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `confirm_status` varchar(32) NOT NULL DEFAULT 'NOT_REQUIRED' COMMENT '确认状态',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_announcement_recipient` (`tenant_id`, `announcement_id`, `user_id`),
  KEY `idx_notice_announcement_user` (`tenant_id`, `user_id`, `read_status`, `created_at`),
  KEY `idx_notice_announcement_confirm` (`tenant_id`, `announcement_id`, `confirm_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告用户接收记录表';
