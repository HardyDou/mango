INSERT INTO `payment_channel`
  (`id`, `channel_code`, `channel_name`, `environment`, `channel_type`, `adapter_type`, `merchant_no`, `gateway_base_url`, `gateway_url`, `field_template_json`, `capability_summary`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (330004, 'OFFLINE_COLLECTION', '线下收款', 'OFFLINE_COLLECTION', 'BUILTIN_OFFLINE', 'OFFLINE_COLLECTION', 'OFFLINE_COLLECTION_MERCHANT_001', '/payment/offline-collections', '/payment/offline-collections', '[
  {"name":"accountName","label":"收款户名","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":1,"group":"收款账户"},
  {"name":"accountNo","label":"收款账号","component":"input","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":2,"group":"收款账户"},
  {"name":"bankName","label":"开户行","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":3,"group":"收款账户"}
]', '线下收款通道，本阶段启用线下转账支付下单、转账备注识别码和转账物料；凭证确认、银行流水导入、对账匹配和线下退款处理按后续线下收款台账项交付', 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `channel_name` = VALUES(`channel_name`),
  `environment` = VALUES(`environment`),
  `channel_type` = VALUES(`channel_type`),
  `adapter_type` = VALUES(`adapter_type`),
  `merchant_no` = VALUES(`merchant_no`),
  `gateway_base_url` = VALUES(`gateway_base_url`),
  `gateway_url` = VALUES(`gateway_url`),
  `field_template_json` = VALUES(`field_template_json`),
  `capability_summary` = VALUES(`capability_summary`),
  `status` = 1,
  `updated_at` = NOW(),
  `del_flag` = 0;

UPDATE `payment_channel_contract`
SET `enabled_method_codes` = 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_H5,CORPORATE_EBANK_REDIRECT,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT',
    `updated_at` = NOW()
WHERE `id` = 331001
  AND `del_flag` = 0;

INSERT INTO `payment_channel_contract`
  (`id`, `contract_code`, `contract_name`, `subject_id`, `channel_id`, `environment`, `merchant_no`, `app_id`, `config_values_json`, `enabled_method_codes`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (331004, 'OFFLINE_COLLECTION_MANGO_TECH', '芒果科技线下收款签约', 320001, 330004, 'OFFLINE_COLLECTION', 'OFFLINE_COLLECTION_MERCHANT_001', 'offline-collection-app', '{"voucherRequired":true,"reconciliationCodeRequired":true}', 'CORPORATE_OFFLINE_ACCOUNT', 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `contract_name` = VALUES(`contract_name`),
  `subject_id` = VALUES(`subject_id`),
  `channel_id` = VALUES(`channel_id`),
  `environment` = VALUES(`environment`),
  `merchant_no` = VALUES(`merchant_no`),
  `app_id` = VALUES(`app_id`),
  `config_values_json` = VALUES(`config_values_json`),
  `enabled_method_codes` = VALUES(`enabled_method_codes`),
  `status` = 1,
  `updated_at` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_method`
  (`id`, `method_code`, `method_name`, `channel_id`, `account_nature`, `instrument_type`, `interaction_type`, `terminal_scope`, `payment_material_type`, `description`, `route_strategy`, `min_amount`, `max_amount`, `sort`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (340009, 'PERSONAL_ALIPAY_QR', '支付宝扫码', NULL, 'PERSONAL', 'ALIPAY', 'QR_CODE', 'WEB', 'QR', '桌面 Web 展示支付宝二维码完成付款', '按租户、应用、主体、终端、金额命中签约能力', 1, 5000000, 9, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (340010, 'PERSONAL_ALIPAY_PC', '支付宝 PC', NULL, 'PERSONAL', 'ALIPAY', 'PC_LOGIN', 'WEB', 'HTML_FORM', '桌面 Web 跳转或提交支付宝 PC 支付表单完成付款', '按租户、应用、主体、终端、金额命中签约能力', 1, 5000000, 10, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (340011, 'PERSONAL_EBANK_REDIRECT', '个人网银', NULL, 'PERSONAL', 'EBANK', 'BANK_GATEWAY', 'WEB', 'HTML_FORM', '选择银行并跳转个人网银完成付款', '按租户、应用、主体、终端、金额命中签约能力', 1, 20000000, 11, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `method_name` = VALUES(`method_name`),
  `channel_id` = VALUES(`channel_id`),
  `account_nature` = VALUES(`account_nature`),
  `instrument_type` = VALUES(`instrument_type`),
  `interaction_type` = VALUES(`interaction_type`),
  `terminal_scope` = VALUES(`terminal_scope`),
  `payment_material_type` = VALUES(`payment_material_type`),
  `description` = VALUES(`description`),
  `route_strategy` = VALUES(`route_strategy`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `sort` = VALUES(`sort`),
  `status` = 1,
  `updated_at` = NOW(),
  `del_flag` = 0;

UPDATE `payment_channel_capability`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `channel_id` = 330001
  AND `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `del_flag` = 0;

UPDATE `payment_channel_contract_capability`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `contract_id` = 331001
  AND `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `del_flag` = 0;

INSERT INTO `payment_channel_capability`
  (`id`, `channel_id`, `method_code`, `terminal_type`, `environment`, `supports_refund`, `supports_query`, `supports_close`, `supports_bill`, `supports_reconcile`, `min_amount`, `max_amount`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (332011, 330001, 'PERSONAL_ALIPAY_QR', 'WEB', 'MANGO_PAY', 1, 1, 1, 1, 1, 1, 5000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (332012, 330001, 'PERSONAL_ALIPAY_PC', 'WEB', 'MANGO_PAY', 1, 1, 1, 1, 1, 1, 5000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (332013, 330001, 'PERSONAL_EBANK_REDIRECT', 'WEB', 'MANGO_PAY', 1, 1, 1, 1, 1, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (332014, 330004, 'CORPORATE_OFFLINE_ACCOUNT', 'WEB', 'OFFLINE_COLLECTION', 0, 0, 0, 0, 0, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `supports_refund` = VALUES(`supports_refund`),
  `supports_query` = VALUES(`supports_query`),
  `supports_close` = VALUES(`supports_close`),
  `supports_bill` = VALUES(`supports_bill`),
  `supports_reconcile` = VALUES(`supports_reconcile`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `status` = 1,
  `updated_at` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_channel_contract_capability`
  (`id`, `contract_id`, `channel_capability_id`, `method_code`, `terminal_type`, `min_amount`, `max_amount`, `priority`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (333011, 331001, 332011, 'PERSONAL_ALIPAY_QR', 'WEB', 1, 5000000, 31, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333012, 331001, 332012, 'PERSONAL_ALIPAY_PC', 'WEB', 1, 5000000, 32, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333013, 331001, 332013, 'PERSONAL_EBANK_REDIRECT', 'WEB', 1, 20000000, 33, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333014, 331004, 332014, 'CORPORATE_OFFLINE_ACCOUNT', 'WEB', 1, 20000000, 10, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `channel_capability_id` = VALUES(`channel_capability_id`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `priority` = VALUES(`priority`),
  `status` = 1,
  `updated_at` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_method_route_rule`
  (`id`, `rule_code`, `rule_name`, `app_id`, `subject_id`, `method_code`, `terminal_type`, `environment`, `route_mode`, `fallback_enabled`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (334003, 'ORDER_CENTER_CORPORATE_EBANK_MANGO_PAY', '订单中心企业网银芒果支付路由', 310001, 320001, 'CORPORATE_EBANK_REDIRECT', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (334004, 'ORDER_CENTER_ALIPAY_QR_MANGO_PAY', '订单中心支付宝扫码芒果支付路由', 310001, 320001, 'PERSONAL_ALIPAY_QR', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (334005, 'ORDER_CENTER_ALIPAY_PC_MANGO_PAY', '订单中心支付宝 PC 芒果支付路由', 310001, 320001, 'PERSONAL_ALIPAY_PC', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (334006, 'ORDER_CENTER_PERSONAL_EBANK_MANGO_PAY', '订单中心个人网银芒果支付路由', 310001, 320001, 'PERSONAL_EBANK_REDIRECT', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (334007, 'ORDER_CENTER_OFFLINE_COLLECTION', '订单中心线下转账线下收款通道路由', 310001, 320001, 'CORPORATE_OFFLINE_ACCOUNT', 'WEB', 'OFFLINE_COLLECTION', 'PRIORITY', 0, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `rule_name` = VALUES(`rule_name`),
  `environment` = VALUES(`environment`),
  `route_mode` = VALUES(`route_mode`),
  `fallback_enabled` = VALUES(`fallback_enabled`),
  `status` = 1,
  `updated_at` = NOW(),
  `del_flag` = 0;

INSERT INTO `payment_method_route_rule_item`
  (`id`, `rule_id`, `contract_capability_id`, `priority`, `weight`, `min_amount`, `max_amount`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (335003, 334003, 333004, 10, 100, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (335004, 334004, 333011, 10, 100, 1, 5000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (335005, 334005, 333012, 10, 100, 1, 5000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (335006, 334006, 333013, 10, 100, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (335007, 334007, 333014, 10, 100, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `contract_capability_id` = VALUES(`contract_capability_id`),
  `priority` = VALUES(`priority`),
  `weight` = VALUES(`weight`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `status` = 1,
  `updated_at` = NOW(),
  `del_flag` = 0;

UPDATE `payment_cashier_config`
SET `method_codes` = 'PERSONAL_ALIPAY_PC,PERSONAL_ALIPAY_QR,PERSONAL_WECHAT_QR,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `method_display_order` = 'PERSONAL_ALIPAY_PC,PERSONAL_ALIPAY_QR,PERSONAL_WECHAT_QR,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `default_method_code` = 'PERSONAL_WECHAT_QR',
    `updated_at` = NOW()
WHERE `id` = 350001
  AND `del_flag` = 0;
