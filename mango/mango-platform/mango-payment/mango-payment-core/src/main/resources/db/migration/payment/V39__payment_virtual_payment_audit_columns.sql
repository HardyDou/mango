ALTER TABLE `payment_virtual_channel_payment`
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

ALTER TABLE `payment_virtual_channel_payment`
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`;

ALTER TABLE `payment_virtual_channel_payment`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  ADD KEY `idx_payment_virtual_tenant_audit_time` (`tenant_id`, `created_at`);
