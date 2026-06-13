ALTER TABLE `payment_order`
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

ALTER TABLE `payment_order`
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`;

ALTER TABLE `payment_order`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  ADD KEY `idx_payment_order_tenant_status_audit_time` (`tenant_id`, `status`, `created_at`),
  ADD KEY `idx_payment_order_cashier_audit_time` (`tenant_id`, `cashier_config_id`, `created_at`);
