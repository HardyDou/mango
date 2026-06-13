SET @tenant_id := 1;

DELETE FROM `payment_exception_order`
WHERE `tenant_id` = @tenant_id
  AND (
    `exception_no` REGEXP '^(EX-E2E-|EX-UI-E2E-|EX-AQ-E2E-|EX-CL-E2E-|EX-NT-)'
    OR `related_order_no` REGEXP '^(PO-EX-E2E-|PO-EX-UI-E2E-|PO-EX-AQ-|PO-EX-CL-)'
    OR `reason` REGEXP '^E2E '
  );

DELETE eo
FROM `payment_exception_order` eo
JOIN (
  SELECT `tenant_id`, `related_order_no`, `exception_type`, MIN(`id`) AS `keep_id`
  FROM `payment_exception_order`
  WHERE `del_flag` = 0
  GROUP BY `tenant_id`, `related_order_no`, `exception_type`
  HAVING COUNT(1) > 1
) duplicate_eo
  ON duplicate_eo.`tenant_id` = eo.`tenant_id`
 AND duplicate_eo.`related_order_no` = eo.`related_order_no`
 AND duplicate_eo.`exception_type` = eo.`exception_type`
WHERE eo.`del_flag` = 0
  AND eo.`id` <> duplicate_eo.`keep_id`;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `payment_exception_order` ADD COLUMN `active_business_key` varchar(256) GENERATED ALWAYS AS (CASE WHEN `del_flag` = 0 THEN CONCAT(`tenant_id`, ''|'', `related_order_no`, ''|'', `exception_type`) ELSE NULL END) STORED COMMENT ''未删除异常订单业务幂等键'' AFTER `exception_type`',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'payment_exception_order'
    AND column_name = 'active_business_key'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `payment_exception_order` ADD UNIQUE KEY `uk_payment_exception_active_business` (`active_business_key`)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'payment_exception_order'
    AND index_name = 'uk_payment_exception_active_business'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `payment_exception_order`
  (`id`, `exception_no`, `related_order_no`, `exception_type`, `severity`, `handle_status`, `reason`, `handle_action`, `handle_reason`, `handle_result`, `handle_evidence`, `handler_id`, `handler_name`, `handle_time`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (812001, 'EX2026060900000001', 'PO2026060900000002', 'PAY_TIMEOUT', 'MEDIUM', 'PENDING', '支付订单超过有效支付时间未收到通道成功结果，已关闭并等待人工核对', NULL, NULL, NULL, NULL, NULL, NULL, NULL, @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (812002, 'EX2026060900000002', 'PO2026060900000005', 'CHANNEL_FAILED', 'HIGH', 'PENDING', '通道支付回调或主动查单返回失败状态，支付订单已失败并等待人工核对失败原因', NULL, NULL, NULL, NULL, NULL, NULL, NULL, @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (812003, 'EX2026060900000003', 'RO2026060900000004', 'REFUND_MISMATCH', 'HIGH', 'PENDING', '通道退款回调或主动查退款返回失败状态，退款订单已失败并等待人工核对退款结果', NULL, NULL, NULL, NULL, NULL, NULL, NULL, @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `related_order_no` = VALUES(`related_order_no`),
  `exception_type` = VALUES(`exception_type`),
  `severity` = VALUES(`severity`),
  `handle_status` = IF(`payment_exception_order`.`handle_status` IN ('PENDING', 'PROCESSING'), VALUES(`handle_status`), `payment_exception_order`.`handle_status`),
  `reason` = VALUES(`reason`),
  `del_flag` = 0,
  `updated_at` = NOW();
