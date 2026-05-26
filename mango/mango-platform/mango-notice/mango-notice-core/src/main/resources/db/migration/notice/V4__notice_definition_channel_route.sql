ALTER TABLE `notice_business_channel_template`
  ADD COLUMN `channel_config_id` bigint DEFAULT NULL COMMENT '绑定渠道配置 ID，空表示 AUTO' AFTER `enabled`;

ALTER TABLE `notice_channel_config`
  ADD COLUMN `weight` int NOT NULL DEFAULT 100 COMMENT 'AUTO 路由权重' AFTER `priority`,
  ADD COLUMN `config_status` varchar(32) NOT NULL DEFAULT 'INCOMPLETE' COMMENT '配置状态' AFTER `weight`,
  ADD COLUMN `last_send_status` varchar(32) NOT NULL DEFAULT 'NONE' COMMENT '最近发送状态' AFTER `config_status`,
  ADD COLUMN `last_send_time` datetime DEFAULT NULL COMMENT '最近发送时间' AFTER `last_send_status`,
  ADD COLUMN `last_failure_code` varchar(64) DEFAULT NULL COMMENT '最近失败码' AFTER `last_send_time`,
  ADD COLUMN `last_failure_reason` varchar(500) DEFAULT NULL COMMENT '最近失败原因' AFTER `last_failure_code`,
  ADD KEY `idx_notice_channel_route` (`tenant_id`, `channel_type`, `enabled`, `config_status`, `weight`);
