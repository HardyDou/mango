SET @tenant_id := 1;

DROP TABLE IF EXISTS `tmp_payment_simulated_business_orders`;
CREATE TABLE `tmp_payment_simulated_business_orders` (
  `biz_order_no` varchar(64) NOT NULL,
  PRIMARY KEY (`biz_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tmp_payment_simulated_business_orders` (`biz_order_no`)
VALUES
  ('BO2026060900000001'),
  ('BO2026060900000002'),
  ('BO2026060900000003'),
  ('BO2026060900000004'),
  ('BO2026060900000005'),
  ('BO2026060900000006'),
  ('BO2026060900000007');

DROP TABLE IF EXISTS `tmp_payment_simulated_payment_orders`;
CREATE TABLE `tmp_payment_simulated_payment_orders` (
  `pay_order_no` varchar(64) NOT NULL,
  PRIMARY KEY (`pay_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tmp_payment_simulated_payment_orders` (`pay_order_no`)
VALUES
  ('PO2026060900000002'),
  ('PO2026060900000003'),
  ('PO2026060900000004'),
  ('PO2026060900000005'),
  ('PO2026060900000006'),
  ('PO2026060900000007');

DROP TABLE IF EXISTS `tmp_payment_nonstandard_business_orders`;
CREATE TABLE `tmp_payment_nonstandard_business_orders` (
  `id` bigint NOT NULL,
  `biz_order_no` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tmp_payment_nonstandard_business_orders_no` (`biz_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tmp_payment_nonstandard_business_orders` (`id`, `biz_order_no`)
SELECT `id`, `biz_order_no`
FROM `payment_business_order`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (360001, 360002, 360003, 361001, 361002, 361003, 361004, 361005, 361006, 361007)
    OR `biz_order_no` IN (SELECT `biz_order_no` FROM `tmp_payment_simulated_business_orders`)
    OR `biz_order_no` REGEXP '^(BO202605|BO-MANGO-LAYOUT-|OFFLINE-UI-|PAY-E2E-|PAY-DELAY-E2E-|SETTLE-BO-|REFUND-E2E-)'
    OR `title` REGEXP '(E2E|测试|OFFLINE-UI|MANGO-LAYOUT)'
  );

DROP TABLE IF EXISTS `tmp_payment_nonstandard_payment_orders`;
CREATE TABLE `tmp_payment_nonstandard_payment_orders` (
  `id` bigint NOT NULL,
  `pay_order_no` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tmp_payment_nonstandard_payment_orders_no` (`pay_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tmp_payment_nonstandard_payment_orders` (`id`, `pay_order_no`)
SELECT `id`, `pay_order_no`
FROM `payment_order`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (370001, 370002, 370003, 371002, 371003, 371004, 371005, 371006, 371007)
    OR `pay_order_no` IN (SELECT `pay_order_no` FROM `tmp_payment_simulated_payment_orders`)
    OR `business_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_business_orders`)
    OR `pay_order_no` REGEXP '^(PO202605|PAY-E2E-|PAY-DELAY-E2E-|SETTLE-PO-|REFUND-E2E-)'
    OR `channel_trade_no` REGEXP '^(MANGO_PAY-T202605|ALLINPAY-T202605|E2E|PAY-E2E|SETTLE-)'
  );

DROP TABLE IF EXISTS `tmp_payment_nonstandard_refund_orders`;
CREATE TABLE `tmp_payment_nonstandard_refund_orders` (
  `id` bigint NOT NULL,
  `refund_order_no` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tmp_payment_nonstandard_refund_orders_no` (`refund_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tmp_payment_nonstandard_refund_orders` (`id`, `refund_order_no`)
SELECT `id`, `refund_order_no`
FROM `payment_refund_order`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (380001, 380002, 381004)
    OR `payment_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `refund_order_no` REGEXP '^(RO202605|RO2026060900000004|REFUND-E2E-)'
    OR `biz_refund_no` REGEXP '^(BR202605|BR2026060900000004|REFUND-E2E-)'
  );

DROP TABLE IF EXISTS `tmp_payment_nonstandard_offline_collections`;
CREATE TABLE `tmp_payment_nonstandard_offline_collections` (
  `id` bigint NOT NULL,
  `offline_collection_no` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tmp_payment_nonstandard_offline_collections_no` (`offline_collection_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tmp_payment_nonstandard_offline_collections` (`id`, `offline_collection_no`)
SELECT `id`, `offline_collection_no`
FROM `payment_offline_collection`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (651005, 651006, 651007)
    OR `payment_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `business_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_business_orders`)
    OR `offline_collection_no` REGEXP '^(OC2026060900000005|OC2026060900000006|OC2026060900000007)'
    OR `reconciliation_code` REGEXP '^RC[0-9]{16,}$'
    OR `transfer_remark` REGEXP '^(PO202606|PO202605|.* RC[0-9]{16,})'
  );

DELETE FROM `payment_offline_collection_match`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (735007)
    OR `offline_collection_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_offline_collections`)
    OR `pay_order_no` IN (SELECT `pay_order_no` FROM `tmp_payment_nonstandard_payment_orders`)
  );

DELETE FROM `payment_offline_bank_statement_item`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (734007)
    OR `matched_offline_collection_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_offline_collections`)
    OR `matched_pay_order_no` IN (SELECT `pay_order_no` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `bank_statement_no` REGEXP '^(SIM-BANK-|E2E|TEST)'
  );

DELETE FROM `payment_offline_bank_statement_batch`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (733007)
    OR `batch_no` REGEXP '^(OB2026060900000007|E2E|TEST)'
    OR `statement_file_name` REGEXP '(E2E|测试|test)'
  );

DELETE FROM `payment_offline_collection_voucher`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (652006)
    OR `offline_collection_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_offline_collections`)
    OR `pay_order_no` IN (SELECT `pay_order_no` FROM `tmp_payment_nonstandard_payment_orders`)
  );

DELETE FROM `payment_offline_refund_process`
WHERE `tenant_id` = @tenant_id
  AND (
    `offline_collection_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_offline_collections`)
    OR `payment_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `business_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_business_orders`)
    OR `pay_order_no` IN (SELECT `pay_order_no` FROM `tmp_payment_nonstandard_payment_orders`)
  );

DELETE FROM `payment_refund_query_record`
WHERE `tenant_id` = @tenant_id
  AND (
    `refund_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_refund_orders`)
    OR `refund_order_no` IN (SELECT `refund_order_no` FROM `tmp_payment_nonstandard_refund_orders`)
  );

DELETE FROM `payment_channel_query_record`
WHERE `tenant_id` = @tenant_id
  AND (
    `payment_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `pay_order_no` IN (SELECT `pay_order_no` FROM `tmp_payment_nonstandard_payment_orders`)
  );

DELETE FROM `payment_notification_record`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (410001, 410002, 411003, 411004, 411007)
    OR `related_order_no` IN (SELECT `pay_order_no` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `notification_no` REGEXP '^(NT202605|NT2026060900000003|NT2026060900000004|NT2026060900000007|E2E|TEST)'
  );

DELETE FROM `payment_order_status_flow`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` BETWEEN 491001 AND 491099
    OR (`order_type` = 'BUSINESS_ORDER' AND `order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_business_orders`))
    OR (`order_type` = 'PAYMENT_ORDER' AND `order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_payment_orders`))
    OR (`order_type` = 'REFUND_ORDER' AND `order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_refund_orders`))
    OR `order_no` IN (SELECT `biz_order_no` FROM `tmp_payment_nonstandard_business_orders`)
    OR `order_no` IN (SELECT `pay_order_no` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `order_no` IN (SELECT `refund_order_no` FROM `tmp_payment_nonstandard_refund_orders`)
  );

DELETE FROM `payment_operation_audit`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (450001, 450002)
    OR `id` BETWEEN 795001 AND 795099
    OR `resource_id` IN (SELECT `biz_order_no` FROM `tmp_payment_nonstandard_business_orders`)
    OR `resource_id` IN (SELECT `pay_order_no` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `resource_id` IN (SELECT `refund_order_no` FROM `tmp_payment_nonstandard_refund_orders`)
    OR `resource_id` IN (SELECT `offline_collection_no` FROM `tmp_payment_nonstandard_offline_collections`)
    OR `resource_id` REGEXP '^(MANGO_PAY|DF202605|E2E|TEST)'
  );

DELETE FROM `payment_transaction_flow`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (390001, 390002, 390003, 391003, 391004, 391007, 391008)
    OR `business_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_business_orders`)
    OR `payment_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `refund_order_id` IN (SELECT `id` FROM `tmp_payment_nonstandard_refund_orders`)
    OR `flow_no` REGEXP '^(FLOW202605|PF2026060900000003|PF2026060900000004|RF2026060900000004|PF2026060900000007|E2E|TEST)'
  );

DELETE FROM `payment_virtual_channel_payment`
WHERE `tenant_id` = @tenant_id
  AND (
    `id` IN (395002, 395003, 395004)
    OR `pay_order_no` IN (SELECT `pay_order_no` FROM `tmp_payment_nonstandard_payment_orders`)
    OR `virtual_payment_no` REGEXP '^(MP2026060900000002|MP2026060900000003|MP2026060900000004|E2E|TEST)'
  );

DELETE FROM `payment_refund_order`
WHERE `tenant_id` = @tenant_id
  AND `id` IN (SELECT `id` FROM `tmp_payment_nonstandard_refund_orders`);

DELETE FROM `payment_offline_collection`
WHERE `tenant_id` = @tenant_id
  AND `id` IN (SELECT `id` FROM `tmp_payment_nonstandard_offline_collections`);

DELETE FROM `payment_order`
WHERE `tenant_id` = @tenant_id
  AND `id` IN (SELECT `id` FROM `tmp_payment_nonstandard_payment_orders`);

DELETE FROM `payment_business_order`
WHERE `tenant_id` = @tenant_id
  AND `id` IN (SELECT `id` FROM `tmp_payment_nonstandard_business_orders`);

UPDATE `payment_method_route_rule_item` `item`
JOIN `payment_method_route_rule` `rule`
  ON `rule`.`id` = `item`.`rule_id`
 AND `rule`.`tenant_id` = `item`.`tenant_id`
SET `item`.`status` = 0,
    `item`.`del_flag` = 1,
    `item`.`updated_at` = NOW()
WHERE `item`.`tenant_id` = @tenant_id
  AND `item`.`del_flag` = 0
  AND (`rule`.`rule_code` REGEXP '(E2E|TEST)' OR `rule`.`id` NOT IN (334001, 334003, 334004, 334005, 334006, 334007, 334008, 334009, 334010, 334011, 334012, 334013));

UPDATE `payment_method_route_rule`
SET `status` = 0,
    `del_flag` = 1,
    `updated_at` = NOW()
WHERE `tenant_id` = @tenant_id
  AND `del_flag` = 0
  AND (`rule_code` REGEXP '(E2E|TEST)' OR `id` NOT IN (334001, 334003, 334004, 334005, 334006, 334007, 334008, 334009, 334010, 334011, 334012, 334013));

DELETE `capability`
FROM `payment_channel_contract_capability` `capability`
JOIN `payment_channel_contract` `contract`
  ON `contract`.`id` = `capability`.`contract_id`
 AND `contract`.`tenant_id` = `capability`.`tenant_id`
WHERE `capability`.`tenant_id` = @tenant_id
  AND (`contract`.`contract_name` REGEXP '(E2E|测试)' OR `contract`.`id` NOT IN (331001, 331002, 331003, 331004, 331005, 331006, 331007, 331008));

UPDATE `payment_channel_contract`
SET `status` = 0,
    `del_flag` = 1,
    `updated_at` = NOW()
WHERE `tenant_id` = @tenant_id
  AND `del_flag` = 0
  AND (`contract_name` REGEXP '(E2E|测试)' OR `id` NOT IN (331001, 331002, 331003, 331004, 331005, 331006, 331007, 331008));

UPDATE `payment_cashier_config`
SET `status` = 0,
    `default_cashier` = 0,
    `del_flag` = 1,
    `updated_at` = NOW()
WHERE `tenant_id` = @tenant_id
  AND `del_flag` = 0
  AND (`cashier_name` REGEXP '(E2E|测试)' OR `id` NOT IN (350001, 350002));

UPDATE `payment_application`
SET `status` = 0,
    `del_flag` = 1,
    `updated_at` = NOW(),
    `update_time` = NOW()
WHERE `tenant_id` = @tenant_id
  AND `del_flag` = 0
  AND (`app_name` REGEXP '(E2E|测试)' OR `id` NOT IN (310001, 310002));

INSERT INTO `payment_business_order`
  (`id`, `biz_order_no`, `app_code`, `title`, `subject_id`, `amount`, `paid_amount`, `refunded_amount`, `currency`, `status`, `expire_time`, `notify_url`, `return_url`, `extend_info`, `tenant_id`, `created_by`, `updated_by`, `created_at`, `updated_at`, `del_flag`)
VALUES
  (361001, 'BO2026060900000001', 'app_order_center', '企业服务订阅费', 320001, 128800, 0, 0, 'CNY', 'TO_PAY', DATE_ADD(NOW(), INTERVAL 2 HOUR), '/payment/demo/order-center/notify', '/payment/cashier-result', JSON_OBJECT('scenario', '待支付业务订单'), @tenant_id, NULL, NULL, NOW(), NOW(), 0),
  (361002, 'BO2026060900000002', 'app_order_center', '营销活动服务费', 320001, 66800, 0, 0, 'CNY', 'PAYING', DATE_ADD(NOW(), INTERVAL 30 MINUTE), '/payment/demo/order-center/notify', '/payment/cashier-result', JSON_OBJECT('scenario', '微信扫码支付中'), @tenant_id, NULL, NULL, NOW(), NOW(), 0),
  (361003, 'BO2026060900000003', 'app_member_center', '会员年费', 320002, 19900, 19900, 0, 'CNY', 'PAID', DATE_ADD(NOW(), INTERVAL 1 DAY), '/payment/demo/member-center/notify', '/payment/cashier-result', JSON_OBJECT('scenario', '支付宝扫码已支付'), @tenant_id, NULL, NULL, NOW(), NOW(), 0),
  (361004, 'BO2026060900000004', 'app_member_center', '企业培训服务费', 320002, 88000, 88000, 33000, 'CNY', 'PARTIAL_REFUNDED', DATE_ADD(NOW(), INTERVAL 1 DAY), '/payment/demo/member-center/notify', '/payment/cashier-result', JSON_OBJECT('scenario', '已支付后部分退款'), @tenant_id, NULL, NULL, NOW(), NOW(), 0),
  (361005, 'BO2026060900000005', 'app_order_center', '线下培训服务费', 320001, 56000, 0, 0, 'CNY', 'PAYING', DATE_ADD(NOW(), INTERVAL 1 DAY), '/payment/demo/order-center/notify', '/payment/cashier-result', JSON_OBJECT('scenario', '线下转账待用户转账'), @tenant_id, NULL, NULL, NOW(), NOW(), 0),
  (361006, 'BO2026060900000006', 'app_order_center', '企业咨询服务费', 320001, 76000, 0, 0, 'CNY', 'PAYING', DATE_ADD(NOW(), INTERVAL 1 DAY), '/payment/demo/order-center/notify', '/payment/cashier-result', JSON_OBJECT('scenario', '线下转账待财务确认'), @tenant_id, NULL, NULL, NOW(), NOW(), 0),
  (361007, 'BO2026060900000007', 'app_order_center', '年度实施服务费', 320001, 126000, 126000, 0, 'CNY', 'PAID', DATE_ADD(NOW(), INTERVAL 1 DAY), '/payment/demo/order-center/notify', '/payment/cashier-result', JSON_OBJECT('scenario', '线下转账已对账'), @tenant_id, NULL, NULL, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `title` = VALUES(`title`),
  `subject_id` = VALUES(`subject_id`),
  `amount` = VALUES(`amount`),
  `paid_amount` = VALUES(`paid_amount`),
  `refunded_amount` = VALUES(`refunded_amount`),
  `status` = VALUES(`status`),
  `expire_time` = VALUES(`expire_time`),
  `notify_url` = VALUES(`notify_url`),
  `return_url` = VALUES(`return_url`),
  `extend_info` = VALUES(`extend_info`),
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_order`
  (`id`, `pay_order_no`, `business_order_id`, `cashier_config_id`, `channel_id`, `channel_code`, `channel_merchant_no`, `contract_id`, `contract_capability_id`, `route_rule_id`, `method_id`, `amount`, `status`, `channel_trade_no`, `payment_material_json`, `success_flag`, `pay_time`, `expire_time`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (371002, 'PO2026060900000002', 361002, 350001, 330001, 'MANGO_PAY', 'MANGO_PAY_MERCHANT_001', 331001, 333001, 334001, 340001, 66800, 'PAYING', 'MANGO-PAY-T2026060900000002', JSON_OBJECT('materialType', 'QR', 'qrContent', 'mango-pay://pay/PO2026060900000002', 'methodCode', 'PERSONAL_WECHAT_QR', 'expireTime', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL 20 MINUTE), '%Y-%m-%d %H:%i:%s')), 0, NULL, DATE_ADD(NOW(), INTERVAL 20 MINUTE), @tenant_id, NULL, NOW(), NULL, NOW()),
  (371003, 'PO2026060900000003', 361003, 350002, 330001, 'MANGO_PAY', 'MANGO_PAY_MERCHANT_002', 331005, 333017, 334009, 340009, 19900, 'SUCCESS', 'MANGO-PAY-T2026060900000003', JSON_OBJECT('materialType', 'QR', 'qrContent', 'mango-pay://pay/PO2026060900000003', 'methodCode', 'PERSONAL_ALIPAY_QR'), 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), @tenant_id, NULL, NOW(), NULL, NOW()),
  (371004, 'PO2026060900000004', 361004, 350002, 330001, 'MANGO_PAY', 'MANGO_PAY_MERCHANT_002', 331005, 333020, 334012, 340004, 88000, 'SUCCESS', 'MANGO-PAY-T2026060900000004', JSON_OBJECT('materialType', 'EBANK_REDIRECT', 'bankCode', 'CMB', 'methodCode', 'CORPORATE_EBANK_REDIRECT'), 1, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), @tenant_id, NULL, NOW(), NULL, NOW()),
  (371005, 'PO2026060900000005', 361005, 350001, 330004, 'OFFLINE_COLLECTION', 'OFFLINE_COLLECTION_MERCHANT_001', 331004, 333014, 334007, 340002, 56000, 'PAYING', NULL, JSON_OBJECT('materialType', 'TRANSFER_ACCOUNT', 'accountName', '芒果科技有限公司', 'accountNoMask', '622200******0001', 'bankName', '招商银行上海分行', 'transferRemark', 'A7K9Q2'), 0, NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), @tenant_id, NULL, NOW(), NULL, NOW()),
  (371006, 'PO2026060900000006', 361006, 350001, 330004, 'OFFLINE_COLLECTION', 'OFFLINE_COLLECTION_MERCHANT_001', 331004, 333014, 334007, 340002, 76000, 'PAYING', NULL, JSON_OBJECT('materialType', 'TRANSFER_ACCOUNT', 'accountName', '芒果科技有限公司', 'accountNoMask', '622200******0001', 'bankName', '招商银行上海分行', 'transferRemark', 'M8P4X1'), 0, NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), @tenant_id, NULL, NOW(), NULL, NOW()),
  (371007, 'PO2026060900000007', 361007, 350001, 330004, 'OFFLINE_COLLECTION', 'OFFLINE_COLLECTION_MERCHANT_001', 331004, 333014, 334007, 340002, 126000, 'SUCCESS', 'OFFLINE-BANK-T2026060900000007', JSON_OBJECT('materialType', 'TRANSFER_ACCOUNT', 'accountName', '芒果科技有限公司', 'accountNoMask', '622200******0001', 'bankName', '招商银行上海分行', 'transferRemark', 'Z3D8N6'), 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY), @tenant_id, NULL, NOW(), NULL, NOW())
ON DUPLICATE KEY UPDATE
  `business_order_id` = VALUES(`business_order_id`),
  `cashier_config_id` = VALUES(`cashier_config_id`),
  `channel_id` = VALUES(`channel_id`),
  `channel_code` = VALUES(`channel_code`),
  `channel_merchant_no` = VALUES(`channel_merchant_no`),
  `contract_id` = VALUES(`contract_id`),
  `contract_capability_id` = VALUES(`contract_capability_id`),
  `route_rule_id` = VALUES(`route_rule_id`),
  `method_id` = VALUES(`method_id`),
  `amount` = VALUES(`amount`),
  `status` = VALUES(`status`),
  `channel_trade_no` = VALUES(`channel_trade_no`),
  `payment_material_json` = VALUES(`payment_material_json`),
  `success_flag` = VALUES(`success_flag`),
  `pay_time` = VALUES(`pay_time`),
  `expire_time` = VALUES(`expire_time`),
  `updated_at` = NOW();

INSERT INTO `payment_refund_order`
  (`id`, `refund_order_no`, `biz_refund_no`, `payment_order_id`, `channel_refund_no`, `refund_amount`, `reason`, `status`, `refund_time`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (381004, 'RO2026060900000004', 'BR2026060900000004', 371004, 'MANGO-PAY-R2026060900000004', 33000, '客户调整培训席位，退还部分费用', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 30 MINUTE), @tenant_id, NULL, NOW(), NULL, NOW())
ON DUPLICATE KEY UPDATE
  `payment_order_id` = VALUES(`payment_order_id`),
  `channel_refund_no` = VALUES(`channel_refund_no`),
  `refund_amount` = VALUES(`refund_amount`),
  `reason` = VALUES(`reason`),
  `status` = VALUES(`status`),
  `refund_time` = VALUES(`refund_time`),
  `updated_at` = NOW();

INSERT INTO `payment_transaction_flow`
  (`id`, `flow_no`, `business_order_id`, `payment_order_id`, `refund_order_id`, `flow_type`, `amount`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (391003, 'PF2026060900000003', 361003, 371003, NULL, 'PAYMENT_SUCCESS', 19900, @tenant_id, NULL, NOW(), NULL, NOW()),
  (391004, 'PF2026060900000004', 361004, 371004, NULL, 'PAYMENT_SUCCESS', 88000, @tenant_id, NULL, NOW(), NULL, NOW()),
  (391005, 'RF2026060900000004', 361004, 371004, 381004, 'REFUND_SUCCESS', -33000, @tenant_id, NULL, NOW(), NULL, NOW()),
  (391007, 'PF2026060900000007', 361007, 371007, NULL, 'PAYMENT_SUCCESS', 126000, @tenant_id, NULL, NOW(), NULL, NOW())
ON DUPLICATE KEY UPDATE
  `business_order_id` = VALUES(`business_order_id`),
  `payment_order_id` = VALUES(`payment_order_id`),
  `refund_order_id` = VALUES(`refund_order_id`),
  `flow_type` = VALUES(`flow_type`),
  `amount` = VALUES(`amount`),
  `updated_at` = NOW();

INSERT INTO `payment_virtual_channel_payment`
  (`id`, `virtual_payment_no`, `pay_order_no`, `channel_trade_no`, `cashier_config_id`, `payment_method_id`, `title`, `amount`, `payer_name`, `status`, `paid_time`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `payment_method_code`)
VALUES
  (395002, 'MP2026060900000002', 'PO2026060900000002', 'MANGO-PAY-T2026060900000002', 350001, 340001, '营销活动服务费', 66800, NULL, 'PAYING', NULL, @tenant_id, NULL, NOW(), NULL, NOW(), 'PERSONAL_WECHAT_QR'),
  (395003, 'MP2026060900000003', 'PO2026060900000003', 'MANGO-PAY-T2026060900000003', 350002, 340009, '会员年费', 19900, '张三', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 HOUR), @tenant_id, NULL, NOW(), NULL, NOW(), 'PERSONAL_ALIPAY_QR'),
  (395004, 'MP2026060900000004', 'PO2026060900000004', 'MANGO-PAY-T2026060900000004', 350002, 340004, '企业培训服务费', 88000, '上海某科技有限公司', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 HOUR), @tenant_id, NULL, NOW(), NULL, NOW(), 'CORPORATE_EBANK_REDIRECT')
ON DUPLICATE KEY UPDATE
  `pay_order_no` = VALUES(`pay_order_no`),
  `channel_trade_no` = VALUES(`channel_trade_no`),
  `cashier_config_id` = VALUES(`cashier_config_id`),
  `payment_method_id` = VALUES(`payment_method_id`),
  `title` = VALUES(`title`),
  `amount` = VALUES(`amount`),
  `payer_name` = VALUES(`payer_name`),
  `status` = VALUES(`status`),
  `paid_time` = VALUES(`paid_time`),
  `payment_method_code` = VALUES(`payment_method_code`),
  `updated_at` = NOW();

INSERT INTO `payment_offline_collection`
  (`id`, `offline_collection_no`, `payment_order_id`, `pay_order_no`, `business_order_id`, `biz_order_no`, `channel_id`, `channel_code`, `contract_id`, `contract_capability_id`, `subject_id`, `subject_name`, `bank_account_id`, `account_name`, `account_no_mask`, `bank_name`, `amount`, `currency`, `transfer_amount`, `voucher_file_ids`, `submitted_time`, `submit_remark`, `confirmed_amount`, `reconciliation_code`, `transfer_remark`, `voucher_count`, `collection_status`, `expire_time`, `confirmed_time`, `confirmed_by`, `confirmed_by_name`, `confirm_remark`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (651005, 'OC2026060900000005', 371005, 'PO2026060900000005', 361005, 'BO2026060900000005', 330004, 'OFFLINE_COLLECTION', 331004, 333014, 320001, '芒果科技有限公司', 1000000001, '芒果科技有限公司', '622200******0001', '招商银行上海分行', 56000, 'CNY', NULL, NULL, NULL, NULL, NULL, 'A7K9Q2', 'A7K9Q2', 0, 'WAITING_TRANSFER', DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, NULL, @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (651006, 'OC2026060900000006', 371006, 'PO2026060900000006', 361006, 'BO2026060900000006', 330004, 'OFFLINE_COLLECTION', 331004, 333014, 320001, '芒果科技有限公司', 1000000001, '芒果科技有限公司', '622200******0001', '招商银行上海分行', 76000, 'CNY', 76000, 'mango-file:9000000006', DATE_SUB(NOW(), INTERVAL 20 MINUTE), '已通过企业网银完成转账', NULL, 'M8P4X1', 'M8P4X1', 1, 'PENDING_CONFIRM', DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, NULL, @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (651007, 'OC2026060900000007', 371007, 'PO2026060900000007', 361007, 'BO2026060900000007', 330004, 'OFFLINE_COLLECTION', 331004, 333014, 320001, '芒果科技有限公司', 1000000001, '芒果科技有限公司', '622200******0001', '招商银行上海分行', 126000, 'CNY', 126000, NULL, NULL, NULL, 126000, 'Z3D8N6', 'Z3D8N6', 0, 'RECONCILED', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 30 MINUTE), 1, '财务管理员', '银行流水导入自动匹配后确认到账', @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `payment_order_id` = VALUES(`payment_order_id`),
  `pay_order_no` = VALUES(`pay_order_no`),
  `business_order_id` = VALUES(`business_order_id`),
  `biz_order_no` = VALUES(`biz_order_no`),
  `channel_id` = VALUES(`channel_id`),
  `channel_code` = VALUES(`channel_code`),
  `contract_id` = VALUES(`contract_id`),
  `contract_capability_id` = VALUES(`contract_capability_id`),
  `subject_id` = VALUES(`subject_id`),
  `subject_name` = VALUES(`subject_name`),
  `bank_account_id` = VALUES(`bank_account_id`),
  `account_name` = VALUES(`account_name`),
  `account_no_mask` = VALUES(`account_no_mask`),
  `bank_name` = VALUES(`bank_name`),
  `amount` = VALUES(`amount`),
  `transfer_amount` = VALUES(`transfer_amount`),
  `voucher_file_ids` = VALUES(`voucher_file_ids`),
  `submitted_time` = VALUES(`submitted_time`),
  `submit_remark` = VALUES(`submit_remark`),
  `confirmed_amount` = VALUES(`confirmed_amount`),
  `reconciliation_code` = VALUES(`reconciliation_code`),
  `transfer_remark` = VALUES(`transfer_remark`),
  `voucher_count` = VALUES(`voucher_count`),
  `collection_status` = VALUES(`collection_status`),
  `expire_time` = VALUES(`expire_time`),
  `confirmed_time` = VALUES(`confirmed_time`),
  `confirmed_by` = VALUES(`confirmed_by`),
  `confirmed_by_name` = VALUES(`confirmed_by_name`),
  `confirm_remark` = VALUES(`confirm_remark`),
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_offline_collection_voucher`
  (`id`, `offline_collection_id`, `offline_collection_no`, `pay_order_no`, `voucher_file_id`, `upload_source`, `uploader_id`, `uploader_name`, `upload_time`, `review_status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (652006, 651006, 'OC2026060900000006', 'PO2026060900000006', 'mango-file:9000000006', 'CASHIER', NULL, '付款用户', DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'SUBMITTED', @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `offline_collection_id` = VALUES(`offline_collection_id`),
  `offline_collection_no` = VALUES(`offline_collection_no`),
  `pay_order_no` = VALUES(`pay_order_no`),
  `upload_source` = VALUES(`upload_source`),
  `uploader_id` = VALUES(`uploader_id`),
  `uploader_name` = VALUES(`uploader_name`),
  `upload_time` = VALUES(`upload_time`),
  `review_status` = VALUES(`review_status`),
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_offline_bank_statement_batch`
  (`id`, `batch_no`, `bank_account_no_mask`, `bank_name`, `statement_file_id`, `statement_file_name`, `file_digest`, `total_count`, `matched_count`, `confirmed_count`, `difference_count`, `batch_status`, `importer_id`, `importer_name`, `import_time`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (733007, 'OB2026060900000007', '622200******0001', '招商银行上海分行', 9000000007, '招商银行流水-20260609.xlsx', 'sha256:payment-offline-statement-20260609-00000007', 1, 1, 1, 0, 'CONFIRMED', 1, '财务管理员', DATE_SUB(NOW(), INTERVAL 40 MINUTE), @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `bank_account_no_mask` = VALUES(`bank_account_no_mask`),
  `bank_name` = VALUES(`bank_name`),
  `statement_file_id` = VALUES(`statement_file_id`),
  `statement_file_name` = VALUES(`statement_file_name`),
  `file_digest` = VALUES(`file_digest`),
  `total_count` = VALUES(`total_count`),
  `matched_count` = VALUES(`matched_count`),
  `confirmed_count` = VALUES(`confirmed_count`),
  `difference_count` = VALUES(`difference_count`),
  `batch_status` = VALUES(`batch_status`),
  `importer_id` = VALUES(`importer_id`),
  `importer_name` = VALUES(`importer_name`),
  `import_time` = VALUES(`import_time`),
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_offline_bank_statement_item`
  (`id`, `batch_id`, `batch_no`, `row_no`, `bank_statement_no`, `bank_account_no_mask`, `bank_name`, `trade_time`, `trade_date`, `amount`, `currency`, `counterparty_name`, `counterparty_account_no_mask`, `summary`, `remark`, `reconciliation_code`, `matched_offline_collection_id`, `matched_offline_collection_no`, `matched_pay_order_no`, `match_status`, `match_message`, `confirmed_time`, `confirmed_by`, `confirmed_by_name`, `confirm_remark`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (734007, 733007, 'OB2026060900000007', 2, 'SIM-BANK-2026060900000007', '622200******0001', '招商银行上海分行', DATE_SUB(NOW(), INTERVAL 45 MINUTE), CURDATE(), 126000, 'CNY', '上海云上科技有限公司', '622848******1298', '服务费转账', 'Z3D8N6 年度实施服务费', 'Z3D8N6', 651007, 'OC2026060900000007', 'PO2026060900000007', 'CONFIRMED', '按转账备注识别码和金额匹配成功', DATE_SUB(NOW(), INTERVAL 30 MINUTE), 1, '财务管理员', '银行流水批量导入确认到账', @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `batch_id` = VALUES(`batch_id`),
  `batch_no` = VALUES(`batch_no`),
  `row_no` = VALUES(`row_no`),
  `bank_account_no_mask` = VALUES(`bank_account_no_mask`),
  `bank_name` = VALUES(`bank_name`),
  `trade_time` = VALUES(`trade_time`),
  `trade_date` = VALUES(`trade_date`),
  `amount` = VALUES(`amount`),
  `counterparty_name` = VALUES(`counterparty_name`),
  `counterparty_account_no_mask` = VALUES(`counterparty_account_no_mask`),
  `summary` = VALUES(`summary`),
  `remark` = VALUES(`remark`),
  `reconciliation_code` = VALUES(`reconciliation_code`),
  `matched_offline_collection_id` = VALUES(`matched_offline_collection_id`),
  `matched_offline_collection_no` = VALUES(`matched_offline_collection_no`),
  `matched_pay_order_no` = VALUES(`matched_pay_order_no`),
  `match_status` = VALUES(`match_status`),
  `match_message` = VALUES(`match_message`),
  `confirmed_time` = VALUES(`confirmed_time`),
  `confirmed_by` = VALUES(`confirmed_by`),
  `confirmed_by_name` = VALUES(`confirmed_by_name`),
  `confirm_remark` = VALUES(`confirm_remark`),
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_offline_collection_match`
  (`id`, `offline_collection_id`, `offline_collection_no`, `bank_statement_item_id`, `bank_statement_no`, `pay_order_no`, `match_rule`, `match_status`, `difference_type`, `match_message`, `confirmed_time`, `confirmed_by`, `confirmed_by_name`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (735007, 651007, 'OC2026060900000007', 734007, 'SIM-BANK-2026060900000007', 'PO2026060900000007', 'RECONCILIATION_CODE_AMOUNT', 'CONFIRMED', NULL, '识别码和金额一致，已确认到账', DATE_SUB(NOW(), INTERVAL 30 MINUTE), 1, '财务管理员', @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `offline_collection_id` = VALUES(`offline_collection_id`),
  `offline_collection_no` = VALUES(`offline_collection_no`),
  `bank_statement_no` = VALUES(`bank_statement_no`),
  `pay_order_no` = VALUES(`pay_order_no`),
  `match_rule` = VALUES(`match_rule`),
  `match_status` = VALUES(`match_status`),
  `difference_type` = VALUES(`difference_type`),
  `match_message` = VALUES(`match_message`),
  `confirmed_time` = VALUES(`confirmed_time`),
  `confirmed_by` = VALUES(`confirmed_by`),
  `confirmed_by_name` = VALUES(`confirmed_by_name`),
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_notification_record`
  (`id`, `notification_no`, `related_order_no`, `notification_type`, `target_url`, `notify_status`, `retry_times`, `scheduled_notify_time`, `next_retry_time`, `payload_json`, `response_code`, `response_message`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (411003, 'NT2026060900000003', 'PO2026060900000003', 'PAYMENT_SUCCESS', '/payment/demo/member-center/notify', 'SUCCESS', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, JSON_OBJECT('payOrderNo', 'PO2026060900000003', 'status', 'SUCCESS'), '200', 'OK', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (411004, 'NT2026060900000004', 'RO2026060900000004', 'REFUND_SUCCESS', '/payment/demo/member-center/refund-notify', 'SUCCESS', 0, DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, JSON_OBJECT('refundOrderNo', 'RO2026060900000004', 'status', 'SUCCESS'), '200', 'OK', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (411007, 'NT2026060900000007', 'PO2026060900000007', 'PAYMENT_SUCCESS', '/payment/demo/order-center/notify', 'SUCCESS', 0, DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, JSON_OBJECT('payOrderNo', 'PO2026060900000007', 'status', 'SUCCESS'), '200', 'OK', @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `related_order_no` = VALUES(`related_order_no`),
  `notification_type` = VALUES(`notification_type`),
  `target_url` = VALUES(`target_url`),
  `notify_status` = VALUES(`notify_status`),
  `retry_times` = VALUES(`retry_times`),
  `scheduled_notify_time` = VALUES(`scheduled_notify_time`),
  `next_retry_time` = VALUES(`next_retry_time`),
  `payload_json` = VALUES(`payload_json`),
  `response_code` = VALUES(`response_code`),
  `response_message` = VALUES(`response_message`),
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_order_status_flow`
  (`id`, `order_type`, `order_id`, `order_no`, `from_status`, `to_status`, `trigger_source`, `trigger_no`, `operator_id`, `operator_name`, `happen_time`, `remark`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (491001, 'BUSINESS_ORDER', 361001, 'BO2026060900000001', NULL, 'TO_PAY', 'SIMULATED_SEED', 'BO2026060900000001', NULL, 'system', NOW(), '仿真业务订单初始化', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491002, 'BUSINESS_ORDER', 361002, 'BO2026060900000002', 'TO_PAY', 'PAYING', 'SIMULATED_SEED', 'PO2026060900000002', NULL, 'system', NOW(), '仿真微信扫码支付中', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491003, 'PAYMENT_ORDER', 371002, 'PO2026060900000002', 'CREATED', 'PAYING', 'SIMULATED_SEED', 'PO2026060900000002', NULL, 'system', NOW(), '仿真微信扫码支付订单创建', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491004, 'BUSINESS_ORDER', 361003, 'BO2026060900000003', 'PAYING', 'PAID', 'CHANNEL_CALLBACK', 'MANGO-PAY-T2026060900000003', NULL, 'system', DATE_SUB(NOW(), INTERVAL 1 HOUR), '芒果支付回调支付成功', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491005, 'PAYMENT_ORDER', 371003, 'PO2026060900000003', 'PAYING', 'SUCCESS', 'CHANNEL_CALLBACK', 'MANGO-PAY-T2026060900000003', NULL, 'system', DATE_SUB(NOW(), INTERVAL 1 HOUR), '芒果支付回调支付成功', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491006, 'BUSINESS_ORDER', 361004, 'BO2026060900000004', 'PAID', 'PARTIAL_REFUNDED', 'REFUND_QUERY', 'MANGO-PAY-R2026060900000004', NULL, 'system', DATE_SUB(NOW(), INTERVAL 30 MINUTE), '退款查询推进部分退款', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491007, 'PAYMENT_ORDER', 371004, 'PO2026060900000004', 'PAYING', 'SUCCESS', 'CHANNEL_CALLBACK', 'MANGO-PAY-T2026060900000004', NULL, 'system', DATE_SUB(NOW(), INTERVAL 2 HOUR), '芒果支付回调支付成功', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491008, 'REFUND_ORDER', 381004, 'RO2026060900000004', 'REFUNDING', 'SUCCESS', 'REFUND_QUERY', 'MANGO-PAY-R2026060900000004', NULL, 'system', DATE_SUB(NOW(), INTERVAL 30 MINUTE), '退款查询推进退款成功', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491009, 'BUSINESS_ORDER', 361005, 'BO2026060900000005', 'TO_PAY', 'PAYING', 'OFFLINE_COLLECTION', 'OC2026060900000005', NULL, 'system', NOW(), '线下转账待用户转账', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491010, 'PAYMENT_ORDER', 371005, 'PO2026060900000005', 'CREATED', 'PAYING', 'OFFLINE_COLLECTION', 'OC2026060900000005', NULL, 'system', NOW(), '线下收款单已生成', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491011, 'BUSINESS_ORDER', 361006, 'BO2026060900000006', 'TO_PAY', 'PAYING', 'OFFLINE_COLLECTION', 'OC2026060900000006', NULL, 'system', NOW(), '线下转账待财务确认', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491012, 'PAYMENT_ORDER', 371006, 'PO2026060900000006', 'CREATED', 'PAYING', 'OFFLINE_COLLECTION', 'OC2026060900000006', NULL, 'system', NOW(), '用户已提交转账信息', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491013, 'BUSINESS_ORDER', 361007, 'BO2026060900000007', 'PAYING', 'PAID', 'OFFLINE_RECONCILIATION', 'OB2026060900000007', 1, '财务管理员', DATE_SUB(NOW(), INTERVAL 30 MINUTE), '线下银行流水对账确认到账', @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (491014, 'PAYMENT_ORDER', 371007, 'PO2026060900000007', 'PAYING', 'SUCCESS', 'OFFLINE_RECONCILIATION', 'OB2026060900000007', 1, '财务管理员', DATE_SUB(NOW(), INTERVAL 30 MINUTE), '线下银行流水对账确认到账', @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `order_type` = VALUES(`order_type`),
  `order_id` = VALUES(`order_id`),
  `order_no` = VALUES(`order_no`),
  `from_status` = VALUES(`from_status`),
  `to_status` = VALUES(`to_status`),
  `trigger_source` = VALUES(`trigger_source`),
  `trigger_no` = VALUES(`trigger_no`),
  `operator_id` = VALUES(`operator_id`),
  `operator_name` = VALUES(`operator_name`),
  `happen_time` = VALUES(`happen_time`),
  `remark` = VALUES(`remark`),
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_operation_audit`
  (`id`, `operator_id`, `operator_name`, `operation_action`, `resource_type`, `resource_id`, `operation_result`, `operation_time`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (795001, NULL, 'system', 'SEED_SIMULATED_RUNTIME_DATA', 'PAYMENT_BUSINESS_ORDER', 'BO2026060900000001', 'SUCCESS', NOW(), @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (795002, NULL, 'system', 'SEED_SIMULATED_RUNTIME_DATA', 'PAYMENT_ORDER', 'PO2026060900000002', 'SUCCESS', NOW(), @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (795003, NULL, 'system', 'SEED_SIMULATED_RUNTIME_DATA', 'PAYMENT_ORDER', 'PO2026060900000003', 'SUCCESS', NOW(), @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (795004, NULL, 'system', 'SEED_SIMULATED_RUNTIME_DATA', 'PAYMENT_REFUND_ORDER', 'RO2026060900000004', 'SUCCESS', NOW(), @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (795005, NULL, 'system', 'SEED_SIMULATED_RUNTIME_DATA', 'PAYMENT_OFFLINE_COLLECTION', 'OC2026060900000005', 'SUCCESS', NOW(), @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (795006, NULL, 'system', 'SEED_SIMULATED_RUNTIME_DATA', 'PAYMENT_OFFLINE_COLLECTION', 'OC2026060900000006', 'SUCCESS', NOW(), @tenant_id, NULL, NOW(), NULL, NOW(), 0),
  (795007, NULL, 'system', 'SEED_SIMULATED_RUNTIME_DATA', 'PAYMENT_OFFLINE_COLLECTION', 'OC2026060900000007', 'SUCCESS', NOW(), @tenant_id, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `operation_action` = VALUES(`operation_action`),
  `resource_type` = VALUES(`resource_type`),
  `resource_id` = VALUES(`resource_id`),
  `operation_result` = VALUES(`operation_result`),
  `operation_time` = NOW(),
  `del_flag` = 0,
  `updated_at` = NOW();

DROP TABLE IF EXISTS `tmp_payment_nonstandard_offline_collections`;
DROP TABLE IF EXISTS `tmp_payment_nonstandard_refund_orders`;
DROP TABLE IF EXISTS `tmp_payment_nonstandard_payment_orders`;
DROP TABLE IF EXISTS `tmp_payment_nonstandard_business_orders`;
DROP TABLE IF EXISTS `tmp_payment_simulated_payment_orders`;
DROP TABLE IF EXISTS `tmp_payment_simulated_business_orders`;
