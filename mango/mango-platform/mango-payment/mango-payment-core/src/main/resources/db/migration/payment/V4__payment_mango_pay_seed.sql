INSERT INTO `payment_application`
  (`id`, `app_code`, `app_name`, `notify_url`, `return_url`, `status`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`, `del_flag`)
VALUES
  (310001, 'ORDER_CENTER', '订单中心', 'http://127.0.0.1:18081/payment/mango-pay/virtual/notify/order-center', 'http://127.0.0.1:5173/#/payment/cashier-result', 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (310002, 'MEMBER_CENTER', '会员中心', 'http://127.0.0.1:18081/payment/mango-pay/virtual/notify/member-center', 'http://127.0.0.1:5173/#/payment/cashier-result', 0, 1, 'system', 'system', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `app_name` = VALUES(`app_name`),
  `notify_url` = VALUES(`notify_url`),
  `return_url` = VALUES(`return_url`),
  `status` = VALUES(`status`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_enterprise_subject`
  (`id`, `subject_name`, `credit_code`, `bank_account_no`, `bank_name`, `license_file_id`, `status`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`, `del_flag`)
VALUES
  (320001, '芒果科技有限公司', '91310000MA1PAY001X', '622200000000000001', '招商银行上海分行', NULL, 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (320002, '芒果服务有限公司', '91310000MA1PAY002X', '622200000000000002', '华夏银行上海分行', NULL, 1, 1, 'system', 'system', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `subject_name` = VALUES(`subject_name`),
  `bank_account_no` = VALUES(`bank_account_no`),
  `bank_name` = VALUES(`bank_name`),
  `status` = VALUES(`status`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_channel`
  (`id`, `channel_code`, `channel_name`, `environment`, `merchant_no`, `gateway_url`, `public_key_ref`, `private_key_ref`, `cert_file_id`, `status`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`, `del_flag`)
VALUES
  (330001, 'MANGO_PAY', '芒果支付', 'CHANNEL_PRODUCT', 'MANGO_PAY_MERCHANT_001', '/payment/mango-pay/virtual', 'mango-pay-public-key-ref', 'mango-pay-private-key-ref', NULL, 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (330002, 'ALLINPAY', '通联支付通道', 'PROD', NULL, NULL, NULL, NULL, NULL, 0, 1, 'system', 'system', NOW(), NOW(), 0),
  (330003, 'HUAXIA_BANK', '华夏银行通道', 'PROD', NULL, NULL, NULL, NULL, NULL, 0, 1, 'system', 'system', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `channel_name` = VALUES(`channel_name`),
  `environment` = VALUES(`environment`),
  `merchant_no` = VALUES(`merchant_no`),
  `gateway_url` = VALUES(`gateway_url`),
  `public_key_ref` = VALUES(`public_key_ref`),
  `private_key_ref` = VALUES(`private_key_ref`),
  `status` = VALUES(`status`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_method`
  (`id`, `method_code`, `method_name`, `channel_id`, `min_amount`, `max_amount`, `sort`, `status`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`, `del_flag`)
VALUES
  (340001, 'PERSONAL_WECHAT_QR', '微信扫码', 330001, 1, 5000000, 1, 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (340002, 'CORPORATE_OFFLINE_ACCOUNT', '对公转账', 330001, 1, 20000000, 2, 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (340003, 'ALLINPAY_H5', '通联 H5 支付', 330002, 1, 5000000, 3, 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (340004, 'HUAXIA_EBANK', '华夏网银支付', 330003, 1, 20000000, 4, 0, 1, 'system', 'system', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `method_name` = VALUES(`method_name`),
  `channel_id` = VALUES(`channel_id`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_cashier_config`
  (`id`, `cashier_name`, `application_id`, `enterprise_subject_id`, `terminal_type`, `method_ids`, `default_method_id`, `expire_minutes`, `result_return_url`, `status`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`, `del_flag`)
VALUES
  (350001, '订单中心 PC 收银台', 310001, 320001, 'PC', '340001,340002', 340001, 30, 'http://127.0.0.1:5173/#/payment/cashier-result', 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (350002, '订单中心 H5 收银台', 310001, 320001, 'H5', '340001,340003', 340001, 15, 'http://127.0.0.1:5173/#/payment/cashier-result', 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (350003, '会员中心 App 收银台', 310002, 320002, 'APP', '340001', 340001, 10, 'http://127.0.0.1:5173/#/payment/cashier-result', 0, 1, 'system', 'system', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `cashier_name` = VALUES(`cashier_name`),
  `application_id` = VALUES(`application_id`),
  `enterprise_subject_id` = VALUES(`enterprise_subject_id`),
  `terminal_type` = VALUES(`terminal_type`),
  `method_ids` = VALUES(`method_ids`),
  `default_method_id` = VALUES(`default_method_id`),
  `expire_minutes` = VALUES(`expire_minutes`),
  `result_return_url` = VALUES(`result_return_url`),
  `status` = VALUES(`status`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_business_order`
  (`id`, `biz_order_no`, `app_code`, `subject_id`, `amount`, `currency`, `status`, `expire_time`, `tenant_id`, `create_time`, `update_time`, `del_flag`)
VALUES
  (360001, 'BO202605250001', 'ORDER_CENTER', 320001, 9900, 'CNY', 'SUCCESS', DATE_ADD(NOW(), INTERVAL 2 HOUR), 1, NOW(), NOW(), 0),
  (360002, 'BO202605250002', 'ORDER_CENTER', 320001, 19900, 'CNY', 'PAYING', DATE_ADD(NOW(), INTERVAL 30 MINUTE), 1, NOW(), NOW(), 0),
  (360003, 'BO202605250003', 'MEMBER_CENTER', 320002, 5900, 'CNY', 'CLOSED', DATE_SUB(NOW(), INTERVAL 1 HOUR), 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `subject_id` = VALUES(`subject_id`),
  `amount` = VALUES(`amount`),
  `status` = VALUES(`status`),
  `expire_time` = VALUES(`expire_time`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_order`
  (`id`, `pay_order_no`, `business_order_id`, `channel_id`, `method_id`, `amount`, `status`, `channel_trade_no`, `tenant_id`, `create_time`, `update_time`)
VALUES
  (370001, 'PO202605250001', 360001, 330001, 340001, 9900, 'SUCCESS', 'MANGO_PAY-T202605250001', 1, NOW(), NOW()),
  (370002, 'PO202605250002', 360002, 330001, 340002, 19900, 'PROCESSING', 'MANGO_PAY-T202605250002', 1, NOW(), NOW()),
  (370003, 'PO202605250003', 360003, 330002, 340003, 5900, 'FAILED', 'ALLINPAY-T202605250003', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `business_order_id` = VALUES(`business_order_id`),
  `channel_id` = VALUES(`channel_id`),
  `method_id` = VALUES(`method_id`),
  `amount` = VALUES(`amount`),
  `status` = VALUES(`status`),
  `channel_trade_no` = VALUES(`channel_trade_no`),
  `update_time` = NOW();

INSERT INTO `payment_refund_order`
  (`id`, `refund_order_no`, `biz_refund_no`, `payment_order_id`, `refund_amount`, `status`, `tenant_id`, `create_time`, `update_time`)
VALUES
  (380001, 'RO202605250001', 'BR202605250001', 370001, 3900, 'SUCCESS', 1, NOW(), NOW()),
  (380002, 'RO202605250002', 'BR202605250002', 370002, 9900, 'PROCESSING', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `payment_order_id` = VALUES(`payment_order_id`),
  `refund_amount` = VALUES(`refund_amount`),
  `status` = VALUES(`status`),
  `update_time` = NOW();

INSERT INTO `payment_transaction_flow`
  (`id`, `flow_no`, `business_order_id`, `payment_order_id`, `refund_order_id`, `flow_type`, `amount`, `tenant_id`, `create_time`)
VALUES
  (390001, 'FLOW202605250001', 360001, 370001, NULL, 'PAYMENT', 9900, 1, NOW()),
  (390002, 'FLOW202605250002', 360001, 370001, 380001, 'REFUND', -3900, 1, NOW()),
  (390003, 'FLOW202605250003', 360002, 370002, NULL, 'PAYMENT_PENDING', 19900, 1, NOW())
ON DUPLICATE KEY UPDATE
  `business_order_id` = VALUES(`business_order_id`),
  `payment_order_id` = VALUES(`payment_order_id`),
  `refund_order_id` = VALUES(`refund_order_id`),
  `flow_type` = VALUES(`flow_type`),
  `amount` = VALUES(`amount`);

INSERT INTO `payment_exception_order`
  (`id`, `exception_no`, `related_order_no`, `exception_type`, `severity`, `handle_status`, `reason`, `handle_result`, `tenant_id`, `create_time`, `update_time`, `del_flag`)
VALUES
  (400001, 'EX202605250001', 'PO202605250003', 'CHANNEL_FAILED', 'HIGH', 'PENDING', '通联返回失败状态', NULL, 1, NOW(), NOW(), 0),
  (400002, 'EX202605250002', 'BO202605250002', 'PAY_TIMEOUT', 'MEDIUM', 'PROCESSING', '订单超过预期支付时长', '已通知业务系统等待补偿', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `exception_type` = VALUES(`exception_type`),
  `severity` = VALUES(`severity`),
  `handle_status` = VALUES(`handle_status`),
  `reason` = VALUES(`reason`),
  `handle_result` = VALUES(`handle_result`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_notification_record`
  (`id`, `notification_no`, `related_order_no`, `notification_type`, `target_url`, `notify_status`, `retry_times`, `next_retry_time`, `response_code`, `response_message`, `tenant_id`, `create_time`, `update_time`, `del_flag`)
VALUES
  (410001, 'NT202605250001', 'PO202605250001', 'PAYMENT_SUCCESS', 'http://127.0.0.1:18081/payment/mango-pay/virtual/notify/order-center', 'SUCCESS', 0, NULL, '200', 'OK', 1, NOW(), NOW(), 0),
  (410002, 'NT202605250002', 'PO202605250003', 'PAYMENT_FAILED', 'http://127.0.0.1:18081/payment/mango-pay/virtual/notify/member-center', 'RETRYING', 2, DATE_ADD(NOW(), INTERVAL 10 MINUTE), '500', '业务系统临时不可用', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `notify_status` = VALUES(`notify_status`),
  `retry_times` = VALUES(`retry_times`),
  `next_retry_time` = VALUES(`next_retry_time`),
  `response_code` = VALUES(`response_code`),
  `response_message` = VALUES(`response_message`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_reconciliation`
  (`id`, `reconciliation_no`, `channel_code`, `bill_date`, `total_count`, `total_amount`, `match_status`, `bill_file_id`, `tenant_id`, `create_time`, `update_time`, `del_flag`)
VALUES
  (420001, 'RC202605250001', 'MANGO_PAY', CURDATE(), 2, 29800, 'MATCHED', NULL, 1, NOW(), NOW(), 0),
  (420002, 'RC202605250002', 'ALLINPAY', CURDATE(), 1, 5900, 'DIFFERENCE', NULL, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `total_count` = VALUES(`total_count`),
  `total_amount` = VALUES(`total_amount`),
  `match_status` = VALUES(`match_status`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_difference`
  (`id`, `difference_no`, `reconciliation_id`, `related_order_no`, `difference_type`, `difference_amount`, `process_status`, `process_result`, `tenant_id`, `create_time`, `update_time`, `del_flag`)
VALUES
  (430001, 'DF202605250001', 420002, 'PO202605250003', 'CHANNEL_MORE', 5900, 'PENDING', NULL, 1, NOW(), NOW(), 0),
  (430002, 'DF202605250002', 420002, 'NT202605250002', 'NOTIFY_FAILED', 0, 'PROCESSING', '等待下一次通知重试', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `difference_type` = VALUES(`difference_type`),
  `difference_amount` = VALUES(`difference_amount`),
  `process_status` = VALUES(`process_status`),
  `process_result` = VALUES(`process_result`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_settlement_summary`
  (`id`, `settlement_date`, `enterprise_subject_id`, `channel_code`, `trade_amount`, `refund_amount`, `fee_amount`, `net_amount`, `tenant_id`, `create_time`, `update_time`, `del_flag`)
VALUES
  (440001, CURDATE(), 320001, 'MANGO_PAY', 29800, 3900, 60, 25840, 1, NOW(), NOW(), 0),
  (440002, CURDATE(), 320002, 'ALLINPAY', 5900, 0, 12, 5888, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `trade_amount` = VALUES(`trade_amount`),
  `refund_amount` = VALUES(`refund_amount`),
  `fee_amount` = VALUES(`fee_amount`),
  `net_amount` = VALUES(`net_amount`),
  `update_time` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_operation_audit`
  (`id`, `operator_id`, `operator_name`, `operation_action`, `resource_type`, `resource_id`, `operation_result`, `operation_time`, `tenant_id`)
VALUES
  (450001, 1, 'admin', 'CREATE_CHANNEL', 'PAYMENT_CHANNEL', 'MANGO_PAY', 'SUCCESS', NOW(), 1),
  (450002, 1, 'admin', 'PROCESS_DIFFERENCE', 'PAYMENT_DIFFERENCE', 'DF202605250002', 'SUCCESS', NOW(), 1)
ON DUPLICATE KEY UPDATE
  `operator_name` = VALUES(`operator_name`),
  `operation_action` = VALUES(`operation_action`),
  `resource_type` = VALUES(`resource_type`),
  `resource_id` = VALUES(`resource_id`),
  `operation_result` = VALUES(`operation_result`),
  `operation_time` = NOW();
