SET @tenant_id := 1;

DELETE FROM `payment_offline_refund_process`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` = 653007
    OR `offline_refund_no` = 'OFR2026060900000007'
    OR `refund_order_id` = 381007
  );

INSERT INTO `payment_offline_refund_process`
  (`id`, `offline_refund_no`, `offline_collection_id`, `offline_collection_no`, `refund_order_id`, `payment_order_id`, `pay_order_no`, `business_order_id`, `biz_order_no`, `channel_id`, `channel_code`, `refund_amount`, `currency`, `refund_account_name`, `refund_account_no_mask`, `refund_bank_name`, `refund_voucher_file_ids`, `refund_voucher_count`, `reason`, `remark`, `refund_status`, `refunded_time`, `operator_id`, `operator_name`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (653007, 'OFR2026060900000007', 651007, 'OC2026060900000007', 381007, 371007, 'PO2026060900000007', 361007, 'BO2026060900000007', 330004, 'OFFLINE_COLLECTION', 26000, 'CNY', '上海云上科技有限公司', '622848******1298', '招商银行上海分行', 'mango-file:9000000017', 1, '客户线下退款', '线下退款凭证确认退款完成', 'REFUNDED', DATE_SUB(NOW(), INTERVAL 10 MINUTE), 1, '财务管理员', @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `offline_collection_id` = VALUES(`offline_collection_id`),
  `offline_collection_no` = VALUES(`offline_collection_no`),
  `refund_order_id` = VALUES(`refund_order_id`),
  `payment_order_id` = VALUES(`payment_order_id`),
  `pay_order_no` = VALUES(`pay_order_no`),
  `business_order_id` = VALUES(`business_order_id`),
  `biz_order_no` = VALUES(`biz_order_no`),
  `channel_id` = VALUES(`channel_id`),
  `channel_code` = VALUES(`channel_code`),
  `refund_amount` = VALUES(`refund_amount`),
  `currency` = VALUES(`currency`),
  `refund_account_name` = VALUES(`refund_account_name`),
  `refund_account_no_mask` = VALUES(`refund_account_no_mask`),
  `refund_bank_name` = VALUES(`refund_bank_name`),
  `refund_voucher_file_ids` = VALUES(`refund_voucher_file_ids`),
  `refund_voucher_count` = VALUES(`refund_voucher_count`),
  `reason` = VALUES(`reason`),
  `remark` = VALUES(`remark`),
  `refund_status` = VALUES(`refund_status`),
  `refunded_time` = VALUES(`refunded_time`),
  `operator_id` = VALUES(`operator_id`),
  `operator_name` = VALUES(`operator_name`),
  `del_flag` = 0,
  `updated_at` = NOW();
