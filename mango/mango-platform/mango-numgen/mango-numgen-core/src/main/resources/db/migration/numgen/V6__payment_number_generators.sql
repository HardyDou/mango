SET @schema_name = DATABASE();

SET @add_generator_domain_code = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_generator` ADD COLUMN `domain_code` varchar(64) NOT NULL DEFAULT ''NUMGEN'' COMMENT ''业务域编码'' AFTER `gen_name`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_generator' AND COLUMN_NAME = 'domain_code'
);
PREPARE stmt FROM @add_generator_domain_code;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_generator_domain_idx = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_generator` ADD KEY `idx_numgen_generator_tenant_domain` (`tenant_id`, `domain_code`, `status`)', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_generator' AND INDEX_NAME = 'idx_numgen_generator_tenant_domain'
);
PREPARE stmt FROM @add_generator_domain_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `numgen_generator`
SET `domain_code` = 'NUMGEN'
WHERE `domain_code` IS NULL OR `domain_code` = '';

CREATE TEMPORARY TABLE `tmp_numgen_payment_seed` (
  `row_no` int NOT NULL,
  `gen_key` varchar(128) NOT NULL,
  `gen_name` varchar(128) NOT NULL,
  `prefix` varchar(16) NOT NULL,
  PRIMARY KEY (`row_no`),
  UNIQUE KEY `uk_tmp_numgen_payment_seed_key` (`gen_key`)
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tmp_numgen_payment_seed` (`row_no`, `gen_key`, `gen_name`, `prefix`) VALUES
  (1, 'PAY_BIZ_ORDER_NO', '支付业务订单号', 'BO'),
  (2, 'PAY_ORDER_NO', '支付订单号', 'PO'),
  (3, 'PAY_REFUND_ORDER_NO', '支付退款订单号', 'RO'),
  (4, 'PAY_BIZ_REFUND_NO', '支付业务退款号', 'BR'),
  (5, 'PAY_REFUND_APPROVAL_NO', '支付退款审批号', 'RA'),
  (6, 'PAY_FLOW_NO', '支付流水号', 'PF'),
  (7, 'PAY_REFUND_FLOW_NO', '支付退款流水号', 'RF'),
  (8, 'PAY_FEE_FLOW_NO', '支付手续费流水号', 'FF'),
  (9, 'PAY_ADJUST_FLOW_NO', '支付调账流水号', 'AF'),
  (10, 'PAY_NOTIFY_NO', '支付通知记录号', 'NT'),
  (11, 'PAY_RECON_BATCH_NO', '支付对账批次号', 'RC'),
  (12, 'PAY_DIFF_NO', '支付对账差异号', 'DF'),
  (13, 'PAY_QUERY_NO', '支付查单记录号', 'PQ'),
  (14, 'PAY_REFUND_QUERY_NO', '支付退款查单记录号', 'RQ'),
  (15, 'PAY_EXCEPTION_NO', '支付异常订单号', 'EX'),
  (16, 'PAY_OFFLINE_COLLECTION_NO', '线下收款单号', 'OC'),
  (17, 'PAY_OFFLINE_REFUND_NO', '线下退款单号', 'OF'),
  (18, 'PAY_OFFLINE_BANK_BATCH_NO', '线下银行流水导入批次号', 'OB'),
  (19, 'PAY_MANGO_VIRTUAL_NO', '芒果支付虚拟付款单号', 'MP'),
  (20, 'PAY_MANGO_SCENARIO_NO', '芒果支付场景控制单号', 'SC');

INSERT INTO `numgen_generator`
  (`id`, `gen_key`, `gen_name`, `domain_code`, `status`, `current_rule_version`, `current_publish_status`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`, `del_flag`)
SELECT
  900000000000 + `row_no`,
  `gen_key`,
  `gen_name`,
  'PAYMENT',
  1,
  1,
  1,
  1,
  'system',
  'system',
  NOW(),
  NOW(),
  0
FROM `tmp_numgen_payment_seed`
ON DUPLICATE KEY UPDATE
  `gen_name` = VALUES(`gen_name`),
  `domain_code` = VALUES(`domain_code`),
  `status` = VALUES(`status`),
  `current_rule_version` = VALUES(`current_rule_version`),
  `current_publish_status` = VALUES(`current_publish_status`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW();

INSERT INTO `numgen_rule`
  (`id`, `gen_key`, `rule_name`, `version`, `status`, `publish_status`, `version_state`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`, `del_flag`)
SELECT
  900000010000 + `row_no`,
  `gen_key`,
  CONCAT(`gen_name`, '默认规则'),
  1,
  1,
  1,
  'ACTIVE',
  1,
  'system',
  'system',
  NOW(),
  NOW(),
  0
FROM `tmp_numgen_payment_seed`
ON DUPLICATE KEY UPDATE
  `rule_name` = VALUES(`rule_name`),
  `status` = VALUES(`status`),
  `publish_status` = VALUES(`publish_status`),
  `version_state` = VALUES(`version_state`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW();

DELETE `segment`
FROM `numgen_rule_segment` `segment`
JOIN `numgen_rule` `rule`
  ON `rule`.`id` = `segment`.`rule_id`
JOIN `tmp_numgen_payment_seed` `seed`
  ON `seed`.`gen_key` = `rule`.`gen_key`
WHERE `rule`.`tenant_id` = 1
  AND `rule`.`version` = 1
  AND `rule`.`del_flag` = 0;

INSERT INTO `numgen_rule_segment`
  (`id`, `rule_id`, `sort_order`, `segment_type`, `segment_name`, `literal_value`, `variable_key`, `date_format`, `seq_width`, `pad_char`, `sequence_scope`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`)
SELECT
  900000020000 + (`row_no` * 10) + 1,
  `rule`.`id`,
  1,
  'TEXT',
  '业务前缀',
  `prefix`,
  NULL,
  NULL,
  NULL,
  '0',
  0,
  1,
  'system',
  'system',
  NOW(),
  NOW()
FROM `tmp_numgen_payment_seed`
JOIN `numgen_rule` `rule`
  ON `rule`.`tenant_id` = 1
 AND `rule`.`gen_key` = `tmp_numgen_payment_seed`.`gen_key`
 AND `rule`.`version` = 1
 AND `rule`.`del_flag` = 0
ON DUPLICATE KEY UPDATE
  `rule_id` = VALUES(`rule_id`),
  `literal_value` = VALUES(`literal_value`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW();

INSERT INTO `numgen_rule_segment`
  (`id`, `rule_id`, `sort_order`, `segment_type`, `segment_name`, `literal_value`, `variable_key`, `date_format`, `seq_width`, `pad_char`, `sequence_scope`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`)
SELECT
  900000020000 + (`row_no` * 10) + 2,
  `rule`.`id`,
  2,
  'DATE',
  '日期',
  NULL,
  NULL,
  'yyyyMMdd',
  NULL,
  '0',
  1,
  1,
  'system',
  'system',
  NOW(),
  NOW()
FROM `tmp_numgen_payment_seed`
JOIN `numgen_rule` `rule`
  ON `rule`.`tenant_id` = 1
 AND `rule`.`gen_key` = `tmp_numgen_payment_seed`.`gen_key`
 AND `rule`.`version` = 1
 AND `rule`.`del_flag` = 0
ON DUPLICATE KEY UPDATE
  `rule_id` = VALUES(`rule_id`),
  `date_format` = VALUES(`date_format`),
  `sequence_scope` = VALUES(`sequence_scope`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW();

INSERT INTO `numgen_rule_segment`
  (`id`, `rule_id`, `sort_order`, `segment_type`, `segment_name`, `literal_value`, `variable_key`, `date_format`, `seq_width`, `pad_char`, `sequence_scope`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`)
SELECT
  900000020000 + (`row_no` * 10) + 3,
  `rule`.`id`,
  3,
  'SEQ',
  '日内序号',
  NULL,
  NULL,
  NULL,
  8,
  '0',
  0,
  1,
  'system',
  'system',
  NOW(),
  NOW()
FROM `tmp_numgen_payment_seed`
JOIN `numgen_rule` `rule`
  ON `rule`.`tenant_id` = 1
 AND `rule`.`gen_key` = `tmp_numgen_payment_seed`.`gen_key`
 AND `rule`.`version` = 1
 AND `rule`.`del_flag` = 0
ON DUPLICATE KEY UPDATE
  `rule_id` = VALUES(`rule_id`),
  `seq_width` = VALUES(`seq_width`),
  `pad_char` = VALUES(`pad_char`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW();

DROP TEMPORARY TABLE `tmp_numgen_payment_seed`;
