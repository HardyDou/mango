ALTER TABLE `payment_offline_refund_process`
  ADD COLUMN `refund_order_id` bigint DEFAULT NULL COMMENT '统一退款订单ID' AFTER `offline_collection_no`;

CREATE INDEX `idx_payment_offline_refund_process_refund_order`
  ON `payment_offline_refund_process` (`tenant_id`, `refund_order_id`);
