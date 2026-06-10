ALTER TABLE `payment_reconciliation`
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

ALTER TABLE `payment_reconciliation`
  ADD COLUMN `total_fee` bigint NOT NULL DEFAULT '0' COMMENT '通道手续费，单位分' AFTER `total_amount`,
  ADD COLUMN `bill_file_name` varchar(255) DEFAULT NULL COMMENT '账单文件名' AFTER `bill_file_id`,
  ADD COLUMN `file_digest` varchar(128) DEFAULT NULL COMMENT '账单文件摘要' AFTER `bill_file_name`,
  ADD COLUMN `importer_id` bigint DEFAULT NULL COMMENT '导入人ID' AFTER `file_digest`,
  ADD COLUMN `importer_name` varchar(128) DEFAULT NULL COMMENT '导入人名称' AFTER `importer_id`,
  ADD COLUMN `import_time` datetime DEFAULT NULL COMMENT '导入时间' AFTER `importer_name`,
  ADD COLUMN `reconcile_result` varchar(512) DEFAULT NULL COMMENT '对账结果说明' AFTER `import_time`,
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`;

UPDATE `payment_reconciliation`
SET `bill_file_name` = COALESCE(`bill_file_name`, CONCAT(`reconciliation_no`, '.bill')),
    `file_digest` = COALESCE(`file_digest`, CONCAT('legacy-', `reconciliation_no`)),
    `import_time` = COALESCE(`import_time`, `created_at`),
    `importer_name` = COALESCE(`importer_name`, 'system'),
    `reconcile_result` = COALESCE(`reconcile_result`, CASE
      WHEN `match_status` = 'MATCHED' THEN '历史批次已平账'
      ELSE '历史批次存在差异'
    END);

ALTER TABLE `payment_reconciliation`
  MODIFY COLUMN `bill_file_name` varchar(255) NOT NULL COMMENT '账单文件名',
  MODIFY COLUMN `file_digest` varchar(128) NOT NULL COMMENT '账单文件摘要',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  ADD UNIQUE KEY `uk_payment_reconciliation_file_digest` (`tenant_id`, `channel_code`, `bill_date`, `file_digest`),
  ADD KEY `idx_payment_reconciliation_tenant_status_time` (`tenant_id`, `match_status`, `created_at`);

CREATE TABLE IF NOT EXISTS `payment_channel_bill_detail` (
  `id` bigint NOT NULL COMMENT '主键',
  `reconciliation_id` bigint NOT NULL COMMENT '对账批次ID',
  `batch_no` varchar(64) NOT NULL COMMENT '账单批次号',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `bill_date` date NOT NULL COMMENT '账单日期',
  `channel_trade_no` varchar(128) NOT NULL COMMENT '通道交易号',
  `trade_type` varchar(32) NOT NULL COMMENT '交易类型：PAYMENT、REFUND、FEE',
  `amount` bigint NOT NULL DEFAULT '0' COMMENT '金额，单位分',
  `fee` bigint NOT NULL DEFAULT '0' COMMENT '手续费，单位分',
  `trade_time` datetime NOT NULL COMMENT '通道交易时间',
  `match_status` varchar(32) NOT NULL COMMENT '匹配状态',
  `matched_order_no` varchar(64) DEFAULT NULL COMMENT '匹配到的订单号',
  `match_message` varchar(512) DEFAULT NULL COMMENT '匹配说明',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_bill_detail_trade` (`tenant_id`, `reconciliation_id`, `channel_trade_no`, `trade_type`),
  KEY `idx_payment_bill_detail_tenant_bill` (`tenant_id`, `channel_code`, `bill_date`),
  KEY `idx_payment_bill_detail_match_status` (`tenant_id`, `match_status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道账单明细';

ALTER TABLE `payment_difference`
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

ALTER TABLE `payment_difference`
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`;

ALTER TABLE `payment_difference`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  ADD KEY `idx_payment_difference_reconciliation` (`tenant_id`, `reconciliation_id`, `difference_type`);
