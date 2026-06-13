ALTER TABLE `payment_channel_capability`
  ADD COLUMN `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除' AFTER `updated_at`;

ALTER TABLE `payment_channel_capability`
  DROP INDEX `uk_payment_channel_capability`,
  ADD UNIQUE KEY `uk_payment_channel_capability` (`tenant_id`, `channel_id`, `method_code`, `terminal_type`, `environment`, `del_flag`),
  ADD KEY `idx_payment_channel_capability_route` (`tenant_id`, `method_code`, `terminal_type`, `environment`, `status`, `del_flag`);
