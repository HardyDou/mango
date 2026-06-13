ALTER TABLE `payment_notification_record`
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

ALTER TABLE `payment_notification_record`
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`,
  ADD COLUMN `last_manual_retry_time` datetime DEFAULT NULL COMMENT '最后人工重推时间' AFTER `response_message`,
  ADD COLUMN `last_manual_retry_reason` varchar(512) DEFAULT NULL COMMENT '最后人工重推原因' AFTER `last_manual_retry_time`,
  ADD COLUMN `last_manual_retry_result` varchar(512) DEFAULT NULL COMMENT '最后人工重推结果' AFTER `last_manual_retry_reason`,
  ADD COLUMN `last_manual_retry_operator_id` bigint DEFAULT NULL COMMENT '最后人工重推人ID' AFTER `last_manual_retry_result`,
  ADD COLUMN `last_manual_retry_operator_name` varchar(128) DEFAULT NULL COMMENT '最后人工重推人名称' AFTER `last_manual_retry_operator_id`;

ALTER TABLE `payment_notification_record`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  ADD KEY `idx_payment_notification_tenant_type_status_time` (`tenant_id`, `notification_type`, `notify_status`, `created_at`);
