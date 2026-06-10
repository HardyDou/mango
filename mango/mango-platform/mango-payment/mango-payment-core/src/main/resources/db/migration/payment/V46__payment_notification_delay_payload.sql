ALTER TABLE `payment_notification_record`
  ADD COLUMN `scheduled_notify_time` datetime DEFAULT NULL COMMENT '计划通知时间' AFTER `retry_times`,
  ADD COLUMN `payload_json` text DEFAULT NULL COMMENT '通知报文快照' AFTER `next_retry_time`;

ALTER TABLE `payment_notification_record`
  ADD KEY `idx_payment_notification_schedule` (`tenant_id`, `notify_status`, `scheduled_notify_time`);
