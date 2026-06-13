ALTER TABLE `payment_difference`
  ADD COLUMN `process_action` varchar(64) DEFAULT NULL COMMENT '处理动作' AFTER `process_status`,
  ADD COLUMN `process_reason` varchar(512) DEFAULT NULL COMMENT '处理原因' AFTER `process_action`,
  ADD COLUMN `process_evidence` varchar(512) DEFAULT NULL COMMENT '处理凭据文件ID或业务凭据token' AFTER `process_result`,
  ADD COLUMN `processor_id` bigint DEFAULT NULL COMMENT '处理人ID' AFTER `process_evidence`,
  ADD COLUMN `processor_name` varchar(128) DEFAULT NULL COMMENT '处理人名称' AFTER `processor_id`,
  ADD COLUMN `process_time` datetime DEFAULT NULL COMMENT '处理时间' AFTER `processor_name`;

ALTER TABLE `payment_difference`
  ADD KEY `idx_payment_difference_tenant_type_status` (`tenant_id`, `difference_type`, `process_status`);
