ALTER TABLE `payment_exception_order`
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

ALTER TABLE `payment_exception_order`
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`,
  ADD COLUMN `handle_action` varchar(64) DEFAULT NULL COMMENT '处理动作' AFTER `reason`,
  ADD COLUMN `handle_reason` varchar(512) DEFAULT NULL COMMENT '处理原因' AFTER `handle_action`,
  ADD COLUMN `handle_evidence` varchar(512) DEFAULT NULL COMMENT '处理凭据' AFTER `handle_result`,
  ADD COLUMN `handler_id` bigint DEFAULT NULL COMMENT '处理人ID' AFTER `handle_evidence`,
  ADD COLUMN `handler_name` varchar(128) DEFAULT NULL COMMENT '处理人名称' AFTER `handler_id`,
  ADD COLUMN `handle_time` datetime DEFAULT NULL COMMENT '处理时间' AFTER `handler_name`;

UPDATE `payment_exception_order`
SET `handle_action` = CASE
      WHEN `handle_status` = 'PROCESSING' THEN 'ACTIVE_QUERY'
      WHEN `handle_status` IN ('HANDLED', 'CLOSED', 'IGNORED') THEN 'MANUAL_REVIEW'
      ELSE `handle_action`
    END,
    `handle_reason` = CASE
      WHEN `handle_status` IN ('PROCESSING', 'HANDLED', 'CLOSED', 'IGNORED') THEN `reason`
      ELSE `handle_reason`
    END
WHERE `handle_action` IS NULL;

ALTER TABLE `payment_exception_order`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  ADD KEY `idx_payment_exception_tenant_type_status_time` (`tenant_id`, `exception_type`, `handle_status`, `created_at`);
