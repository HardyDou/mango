ALTER TABLE `payment_settlement_summary`
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

ALTER TABLE `payment_settlement_summary`
  ADD COLUMN `app_code` varchar(64) NOT NULL DEFAULT '' COMMENT '应用编码' AFTER `settlement_date`,
  ADD COLUMN `trade_count` int NOT NULL DEFAULT '0' COMMENT '支付成功笔数' AFTER `net_amount`,
  ADD COLUMN `refund_count` int NOT NULL DEFAULT '0' COMMENT '退款成功笔数' AFTER `trade_count`,
  ADD COLUMN `unresolved_difference_count` int NOT NULL DEFAULT '0' COMMENT '未解决差异笔数' AFTER `refund_count`,
  ADD COLUMN `unresolved_difference_amount` bigint NOT NULL DEFAULT '0' COMMENT '未解决差异金额，单位分' AFTER `unresolved_difference_count`,
  ADD COLUMN `status` varchar(32) NOT NULL DEFAULT 'GENERATED' COMMENT '状态：GENERATED、CONFIRMED、VOIDED' AFTER `unresolved_difference_amount`,
  ADD COLUMN `generated_by` bigint DEFAULT NULL COMMENT '生成人ID' AFTER `status`,
  ADD COLUMN `generated_by_name` varchar(128) DEFAULT NULL COMMENT '生成人名称' AFTER `generated_by`,
  ADD COLUMN `generated_at` datetime DEFAULT NULL COMMENT '生成时间' AFTER `generated_by_name`,
  ADD COLUMN `confirmed_by` bigint DEFAULT NULL COMMENT '确认人ID' AFTER `generated_at`,
  ADD COLUMN `confirmed_by_name` varchar(128) DEFAULT NULL COMMENT '确认人名称' AFTER `confirmed_by`,
  ADD COLUMN `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间' AFTER `confirmed_by_name`,
  ADD COLUMN `voided_by` bigint DEFAULT NULL COMMENT '作废人ID' AFTER `confirmed_at`,
  ADD COLUMN `voided_by_name` varchar(128) DEFAULT NULL COMMENT '作废人名称' AFTER `voided_by`,
  ADD COLUMN `voided_at` datetime DEFAULT NULL COMMENT '作废时间' AFTER `voided_by_name`,
  ADD COLUMN `void_reason` varchar(512) DEFAULT NULL COMMENT '作废原因' AFTER `voided_at`,
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`;

UPDATE `payment_settlement_summary`
SET `app_code` = COALESCE(NULLIF(`app_code`, ''), 'UNKNOWN'),
    `trade_count` = COALESCE(`trade_count`, 0),
    `refund_count` = COALESCE(`refund_count`, 0),
    `unresolved_difference_count` = COALESCE(`unresolved_difference_count`, 0),
    `unresolved_difference_amount` = COALESCE(`unresolved_difference_amount`, 0),
    `status` = COALESCE(NULLIF(`status`, ''), 'GENERATED'),
    `generated_at` = COALESCE(`generated_at`, `created_at`);

ALTER TABLE `payment_settlement_summary`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  DROP INDEX `uk_payment_settlement_summary`,
  ADD KEY `idx_payment_settlement_scope` (`tenant_id`, `settlement_date`, `app_code`, `enterprise_subject_id`, `channel_code`),
  ADD KEY `idx_payment_settlement_tenant_status_date` (`tenant_id`, `status`, `settlement_date`);
