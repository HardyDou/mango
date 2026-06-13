ALTER TABLE `payment_refund_order`
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

ALTER TABLE `payment_refund_order`
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`,
  ADD COLUMN `channel_refund_no` varchar(128) DEFAULT NULL COMMENT '通道退款单号' AFTER `payment_order_id`,
  ADD COLUMN `reason` varchar(512) DEFAULT NULL COMMENT '退款原因' AFTER `refund_amount`,
  ADD COLUMN `refund_time` datetime DEFAULT NULL COMMENT '退款成功时间' AFTER `status`;

UPDATE `payment_refund_order`
SET `refund_time` = CASE
      WHEN `status` IN ('SUCCESS', 'REFUNDED') THEN COALESCE(`refund_time`, `updated_at`)
      ELSE `refund_time`
    END,
    `reason` = COALESCE(`reason`, '业务退款申请')
WHERE `reason` IS NULL OR `refund_time` IS NULL;

ALTER TABLE `payment_refund_order`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  ADD KEY `idx_payment_refund_tenant_status_time` (`tenant_id`, `status`, `created_at`);
