ALTER TABLE `payment_transaction_flow`
  RENAME COLUMN `create_time` TO `created_at`;

ALTER TABLE `payment_transaction_flow`
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`,
  ADD COLUMN `updated_at` datetime NULL COMMENT '更新时间' AFTER `updated_by`;

UPDATE `payment_transaction_flow`
SET `updated_at` = `created_at`
WHERE `updated_at` IS NULL;

UPDATE `payment_transaction_flow`
SET `flow_type` = CASE
      WHEN `flow_type` = 'PAYMENT' THEN 'PAY_SUCCESS'
      WHEN `flow_type` = 'REFUND' THEN 'REFUND_SUCCESS'
      WHEN `flow_type` = 'PAYMENT_PENDING' THEN 'ADJUST_NOTE'
      WHEN `flow_type` = 'REFUND_PENDING' THEN 'ADJUST_NOTE'
      ELSE `flow_type`
    END,
    `amount` = CASE
      WHEN `flow_type` IN ('PAYMENT_PENDING', 'REFUND_PENDING') THEN 0
      ELSE `amount`
    END;

ALTER TABLE `payment_transaction_flow`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  ADD KEY `idx_payment_flow_tenant_type_audit_time` (`tenant_id`, `flow_type`, `created_at`);
