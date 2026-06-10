ALTER TABLE `payment_difference`
  ADD COLUMN `adjust_flow_id` bigint DEFAULT NULL COMMENT '差异处理备注流水ID' AFTER `process_evidence`,
  ADD COLUMN `adjust_flow_no` varchar(64) DEFAULT NULL COMMENT '差异处理备注流水号' AFTER `adjust_flow_id`;

ALTER TABLE `payment_difference`
  ADD KEY `idx_payment_difference_adjust_flow` (`tenant_id`, `adjust_flow_no`);
