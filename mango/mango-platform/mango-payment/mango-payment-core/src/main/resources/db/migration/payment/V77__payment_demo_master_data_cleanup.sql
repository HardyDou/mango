UPDATE `payment_application`
SET `del_flag` = 1,
    `updated_at` = NOW(),
    `update_time` = NOW()
WHERE `tenant_id` = 1
  AND `id` NOT IN (310001, 310002)
  AND `del_flag` = 0;

UPDATE `payment_cashier_config`
SET `del_flag` = 1,
    `default_cashier` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `id` NOT IN (350001, 350002)
  AND `del_flag` = 0;

UPDATE `payment_enterprise_subject`
SET `del_flag` = 1,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `id` NOT IN (320001, 320002)
  AND `del_flag` = 0;

UPDATE `payment_subject_bank_account`
SET `del_flag` = 1,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `subject_id` NOT IN (320001, 320002)
  AND `del_flag` = 0;

UPDATE `payment_channel`
SET `del_flag` = 1,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `id` NOT IN (330001, 330002, 330003, 330004)
  AND `del_flag` = 0;

UPDATE `payment_channel_contract_value` `value`
JOIN `payment_channel_contract` `contract`
  ON `contract`.`id` = `value`.`contract_id`
 AND `contract`.`tenant_id` = `value`.`tenant_id`
SET `value`.`del_flag` = 1,
    `value`.`updated_at` = NOW()
WHERE `value`.`tenant_id` = 1
  AND `value`.`del_flag` = 0
  AND `contract`.`id` NOT IN (331001, 331002, 331003, 331004, 331005, 331006, 331007, 331008);

UPDATE `payment_channel_contract_capability` `capability`
JOIN `payment_channel_contract` `contract`
  ON `contract`.`id` = `capability`.`contract_id`
 AND `contract`.`tenant_id` = `capability`.`tenant_id`
SET `capability`.`status` = 0,
    `capability`.`updated_at` = NOW()
WHERE `capability`.`tenant_id` = 1
  AND `capability`.`del_flag` = 0
  AND `contract`.`id` NOT IN (331001, 331002, 331003, 331004, 331005, 331006, 331007, 331008);

UPDATE `payment_channel_contract`
SET `del_flag` = 1,
    `status` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `id` NOT IN (331001, 331002, 331003, 331004, 331005, 331006, 331007, 331008)
  AND `del_flag` = 0;

UPDATE `payment_method_route_rule_item` `item`
JOIN `payment_method_route_rule` `rule`
  ON `rule`.`id` = `item`.`rule_id`
 AND `rule`.`tenant_id` = `item`.`tenant_id`
SET `item`.`status` = 0,
    `item`.`updated_at` = NOW()
WHERE `item`.`tenant_id` = 1
  AND `item`.`del_flag` = 0
  AND `rule`.`rule_code` LIKE '%E2E%';

UPDATE `payment_method_route_rule`
SET `del_flag` = 1,
    `status` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `id` NOT IN (334001, 334003, 334004, 334005, 334006, 334007, 334008, 334009, 334010, 334011, 334012, 334013)
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `del_flag` = 1,
    `status` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `method_code` LIKE '%E2E%'
  AND `del_flag` = 0;

UPDATE `payment_application`
SET `app_id` = 'app_order_center',
    `app_name` = '订单中心示例应用',
    `status` = 1,
    `demo_app` = 1,
    `sign_algorithm` = 'HMAC_SHA256',
    `ip_whitelist_enabled` = 0,
    `payload_encrypt_enabled` = 0,
    `secret_configured` = 1,
    `del_flag` = 0,
    `updated_at` = NOW(),
    `update_time` = NOW()
WHERE `id` = 310001
  AND `tenant_id` = 1;

UPDATE `payment_application`
SET `app_id` = 'app_member_center',
    `app_name` = '会员中心示例应用',
    `status` = 1,
    `demo_app` = 1,
    `sign_algorithm` = 'HMAC_SHA256',
    `ip_whitelist_enabled` = 0,
    `payload_encrypt_enabled` = 0,
    `secret_configured` = 1,
    `del_flag` = 0,
    `updated_at` = NOW(),
    `update_time` = NOW()
WHERE `id` = 310002
  AND `tenant_id` = 1;

UPDATE `payment_enterprise_subject`
SET `subject_name` = '芒果科技有限公司',
    `bank_name` = '招商银行上海分行',
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 320001
  AND `tenant_id` = 1;

UPDATE `payment_enterprise_subject`
SET `subject_name` = '芒果服务有限公司',
    `bank_name` = '华夏银行上海分行',
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 320002
  AND `tenant_id` = 1;

UPDATE `payment_subject_bank_account`
SET `account_name` = '芒果科技有限公司',
    `bank_name` = '招商银行上海分行',
    `default_account` = 1,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 1000000001
  AND `tenant_id` = 1;

UPDATE `payment_subject_bank_account`
SET `account_name` = '芒果服务有限公司',
    `bank_name` = '华夏银行上海分行',
    `default_account` = 1,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 1000000002
  AND `tenant_id` = 1;

UPDATE `payment_channel`
SET `channel_name` = '芒果支付',
    `environment` = 'CHANNEL_PRODUCT',
    `channel_type` = 'BUILTIN_VIRTUAL',
    `adapter_type` = 'MANGO_PAY',
    `merchant_no` = 'MANGO_PAY_MERCHANT_001',
    `gateway_base_url` = '/payment/mango-pay/virtual',
    `gateway_url` = '/payment/mango-pay/virtual',
    `capability_summary` = '内置虚拟支付通道，用于开发、内测和支付闭环验证，不允许删除',
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 330001
  AND `tenant_id` = 1;

UPDATE `payment_channel`
SET `channel_name` = '通联支付',
    `environment` = 'PROD',
    `channel_type` = 'AGGREGATOR',
    `adapter_type` = 'ALLINPAY',
    `merchant_no` = 'ALLINPAY_MERCHANT_001',
    `capability_summary` = '通联支付通道，可按真实签约资料维护支付、退款、查单、账单和对账能力',
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 330002
  AND `tenant_id` = 1;

UPDATE `payment_channel`
SET `channel_name` = '华夏银行',
    `environment` = 'PROD',
    `channel_type` = 'BANK',
    `adapter_type` = 'HUAXIA_BANK',
    `merchant_no` = 'HUAXIA_MERCHANT_001',
    `capability_summary` = '华夏银行通道，可按真实签约资料维护企业网银、退款、查单、账单和对账能力',
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 330003
  AND `tenant_id` = 1;

UPDATE `payment_channel`
SET `channel_name` = '线下收款',
    `environment` = 'OFFLINE_COLLECTION',
    `channel_type` = 'BUILTIN_OFFLINE',
    `adapter_type` = 'OFFLINE_COLLECTION',
    `merchant_no` = 'OFFLINE_COLLECTION_MERCHANT_001',
    `gateway_base_url` = '/payment/offline-collections',
    `gateway_url` = '/payment/offline-collections',
    `capability_summary` = '内置线下收款通道，支持线下转账、凭证提交、财务确认到账和银行流水批量对账，不允许删除',
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 330004
  AND `tenant_id` = 1;

UPDATE `payment_method`
SET `status` = CASE
      WHEN `method_code` IN (
        'PERSONAL_WECHAT_QR',
        'PERSONAL_ALIPAY_QR',
        'PERSONAL_ALIPAY_PC',
        'PERSONAL_EBANK_REDIRECT',
        'CORPORATE_EBANK_REDIRECT',
        'CORPORATE_OFFLINE_ACCOUNT'
      ) THEN 1
      ELSE 0
    END,
    `del_flag` = CASE
      WHEN `method_code` IN (
        'PERSONAL_WECHAT_QR',
        'PERSONAL_ALIPAY_QR',
        'PERSONAL_ALIPAY_PC',
        'PERSONAL_EBANK_REDIRECT',
        'CORPORATE_EBANK_REDIRECT',
        'CORPORATE_OFFLINE_ACCOUNT'
      ) THEN 0
      ELSE 1
    END,
    `updated_at` = NOW()
WHERE `tenant_id` = 1;

UPDATE `payment_method`
SET `method_name` = '微信扫码',
    `cashier_group_code` = 'WECHAT_PAY',
    `cashier_group_name` = '微信支付',
    `cashier_group_sort` = 10,
    `sort` = 1,
    `description` = '微信客户端扫码完成支付',
    `requires_qr_refresh` = 1
WHERE `method_code` = 'PERSONAL_WECHAT_QR'
  AND `tenant_id` = 1;

UPDATE `payment_method`
SET `method_name` = '支付宝扫码',
    `cashier_group_code` = 'ALIPAY',
    `cashier_group_name` = '支付宝支付',
    `cashier_group_sort` = 20,
    `sort` = 2,
    `description` = '支付宝客户端扫码完成支付',
    `requires_qr_refresh` = 1
WHERE `method_code` = 'PERSONAL_ALIPAY_QR'
  AND `tenant_id` = 1;

UPDATE `payment_method`
SET `method_name` = '支付宝账户支付',
    `cashier_group_code` = 'ALIPAY',
    `cashier_group_name` = '支付宝支付',
    `cashier_group_sort` = 20,
    `sort` = 3,
    `description` = '支付宝账户支付'
WHERE `method_code` = 'PERSONAL_ALIPAY_PC'
  AND `tenant_id` = 1;

UPDATE `payment_method`
SET `method_name` = '个人网银',
    `cashier_group_code` = 'EBANK',
    `cashier_group_name` = '网银支付',
    `cashier_group_sort` = 30,
    `sort` = 4,
    `requires_bank_selection` = 1,
    `description` = '选择银行后进入个人网银支付'
WHERE `method_code` = 'PERSONAL_EBANK_REDIRECT'
  AND `tenant_id` = 1;

UPDATE `payment_method`
SET `method_name` = '企业网银',
    `cashier_group_code` = 'EBANK',
    `cashier_group_name` = '网银支付',
    `cashier_group_sort` = 30,
    `sort` = 5,
    `requires_bank_selection` = 1,
    `description` = '选择银行后进入企业网银支付'
WHERE `method_code` = 'CORPORATE_EBANK_REDIRECT'
  AND `tenant_id` = 1;

UPDATE `payment_method`
SET `method_name` = '线下转账',
    `cashier_group_code` = 'OFFLINE_TRANSFER',
    `cashier_group_name` = '线下转账',
    `cashier_group_sort` = 40,
    `sort` = 6,
    `description` = '获取收款账户和转账备注，转账后提交凭证'
WHERE `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `tenant_id` = 1;

UPDATE `payment_cashier_config`
SET `del_flag` = 0,
    `status` = 1,
    `default_cashier` = 1,
    `cashier_name` = '订单中心 Web 收银台',
    `application_id` = 310001,
    `enterprise_subject_ids` = '320001',
    `method_codes` = 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `method_display_order` = 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `default_method_code` = 'PERSONAL_WECHAT_QR',
    `result_return_url` = '/payment/cashier-result',
    `display_config` = JSON_OBJECT('subtitle', '请确认订单金额后选择支付方式'),
    `updated_at` = NOW()
WHERE `id` = 350001
  AND `tenant_id` = 1;

UPDATE `payment_cashier_config`
SET `del_flag` = 0,
    `status` = 1,
    `default_cashier` = 1,
    `cashier_name` = '会员中心 Web 收银台',
    `application_id` = 310002,
    `enterprise_subject_ids` = '320002',
    `method_codes` = 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `method_display_order` = 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `default_method_code` = 'PERSONAL_WECHAT_QR',
    `result_return_url` = '/payment/cashier-result',
    `display_config` = JSON_OBJECT('subtitle', '请确认订单金额后选择支付方式'),
    `updated_at` = NOW()
WHERE `id` = 350002
  AND `tenant_id` = 1;

UPDATE `payment_channel_contract`
SET `contract_name` = '芒果科技芒果支付签约',
    `subject_id` = 320001,
    `channel_id` = 330001,
    `environment` = 'MANGO_PAY',
    `merchant_no` = 'MANGO_PAY_MERCHANT_001',
    `enabled_method_codes` = 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT',
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 331001
  AND `tenant_id` = 1;

UPDATE `payment_channel_contract`
SET `contract_name` = '芒果科技通联支付签约',
    `subject_id` = 320001,
    `channel_id` = 330002,
    `environment` = 'PROD',
    `merchant_no` = 'ALLINPAY_MERCHANT_001',
    `enabled_method_codes` = 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT',
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 331002
  AND `tenant_id` = 1;

INSERT INTO `payment_channel_contract`
  (`id`, `contract_code`, `contract_name`, `subject_id`, `channel_id`, `environment`, `merchant_no`, `app_id`, `config_values_json`, `enabled_method_codes`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (331003, 'HUAXIA_BANK_MANGO_TECH', '芒果科技华夏银行签约', 320001, 330003, 'PROD', 'HUAXIA_MERCHANT_001', 'huaxia-bank-app', '{}', 'CORPORATE_EBANK_REDIRECT', 1, 1, NULL, NOW(), NULL, NOW(), 0)
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
  `del_flag` = 0,
  `updated_at` = NOW();

UPDATE `payment_channel_contract`
SET `contract_name` = '芒果科技线下收款签约',
    `subject_id` = 320001,
    `channel_id` = 330004,
    `environment` = 'OFFLINE_COLLECTION',
    `merchant_no` = 'OFFLINE_COLLECTION_MERCHANT_001',
    `enabled_method_codes` = 'CORPORATE_OFFLINE_ACCOUNT',
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `id` = 331004
  AND `tenant_id` = 1;

INSERT INTO `payment_channel_contract`
  (`id`, `contract_code`, `contract_name`, `subject_id`, `channel_id`, `environment`, `merchant_no`, `app_id`, `config_values_json`, `enabled_method_codes`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (331005, 'MANGO_PAY_MANGO_SERVICE', '芒果服务芒果支付签约', 320002, 330001, 'MANGO_PAY', 'MANGO_PAY_MERCHANT_002', 'mango-pay-service-app', '{}', 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT', 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (331006, 'ALLINPAY_MANGO_SERVICE', '芒果服务通联支付签约', 320002, 330002, 'PROD', 'ALLINPAY_MERCHANT_002', 'allinpay-service-app', '{}', 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT', 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (331007, 'HUAXIA_BANK_MANGO_SERVICE', '芒果服务华夏银行签约', 320002, 330003, 'PROD', 'HUAXIA_MERCHANT_002', 'huaxia-bank-service-app', '{}', 'CORPORATE_EBANK_REDIRECT', 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (331008, 'OFFLINE_COLLECTION_MANGO_SERVICE', '芒果服务线下收款签约', 320002, 330004, 'OFFLINE_COLLECTION', 'OFFLINE_COLLECTION_MERCHANT_002', 'offline-collection-service-app', '{"voucherRequired":true,"reconciliationCodeRequired":true}', 'CORPORATE_OFFLINE_ACCOUNT', 1, 1, NULL, NOW(), NULL, NOW(), 0)
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
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_channel_capability`
  (`id`, `channel_id`, `method_code`, `terminal_type`, `environment`, `supports_refund`, `supports_query`, `supports_close`, `supports_bill`, `supports_reconcile`, `min_amount`, `max_amount`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (332015, 330003, 'CORPORATE_EBANK_REDIRECT', 'WEB', 'PROD', 1, 1, 1, 1, 1, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `supports_refund` = VALUES(`supports_refund`),
  `supports_query` = VALUES(`supports_query`),
  `supports_close` = VALUES(`supports_close`),
  `supports_bill` = VALUES(`supports_bill`),
  `supports_reconcile` = VALUES(`supports_reconcile`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `status` = 1,
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_channel_contract_capability`
  (`id`, `contract_id`, `channel_capability_id`, `method_code`, `terminal_type`, `min_amount`, `max_amount`, `priority`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (333015, 331003, 332015, 'CORPORATE_EBANK_REDIRECT', 'WEB', 1, 20000000, 20, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333016, 331005, 332001, 'PERSONAL_WECHAT_QR', 'WEB', 1, 5000000, 10, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333017, 331005, 332011, 'PERSONAL_ALIPAY_QR', 'WEB', 1, 5000000, 11, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333018, 331005, 332012, 'PERSONAL_ALIPAY_PC', 'WEB', 1, 5000000, 12, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333019, 331005, 332013, 'PERSONAL_EBANK_REDIRECT', 'WEB', 1, 20000000, 13, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333020, 331005, 332004, 'CORPORATE_EBANK_REDIRECT', 'WEB', 1, 20000000, 14, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333021, 331007, 332015, 'CORPORATE_EBANK_REDIRECT', 'WEB', 1, 20000000, 20, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333022, 331008, 332014, 'CORPORATE_OFFLINE_ACCOUNT', 'WEB', 1, 20000000, 10, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `channel_capability_id` = VALUES(`channel_capability_id`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `priority` = VALUES(`priority`),
  `status` = 1,
  `del_flag` = 0,
  `updated_at` = NOW();

UPDATE `payment_channel_capability`
SET `status` = CASE
      WHEN (`channel_id` = 330001 AND `method_code` IN ('PERSONAL_WECHAT_QR', 'PERSONAL_ALIPAY_QR', 'PERSONAL_ALIPAY_PC', 'PERSONAL_EBANK_REDIRECT', 'CORPORATE_EBANK_REDIRECT'))
        OR (`channel_id` = 330004 AND `method_code` = 'CORPORATE_OFFLINE_ACCOUNT')
        OR (`channel_id` = 330003 AND `method_code` = 'CORPORATE_EBANK_REDIRECT')
      THEN 1
      ELSE 0
    END,
    `del_flag` = CASE
      WHEN (`channel_id` = 330001 AND `method_code` IN ('PERSONAL_WECHAT_QR', 'PERSONAL_ALIPAY_QR', 'PERSONAL_ALIPAY_PC', 'PERSONAL_EBANK_REDIRECT', 'CORPORATE_EBANK_REDIRECT'))
        OR (`channel_id` = 330004 AND `method_code` = 'CORPORATE_OFFLINE_ACCOUNT')
        OR (`channel_id` = 330003 AND `method_code` = 'CORPORATE_EBANK_REDIRECT')
      THEN 0
      ELSE 1
    END,
    `updated_at` = NOW()
WHERE `tenant_id` = 1;

UPDATE `payment_channel_contract_capability`
SET `status` = CASE
      WHEN `id` IN (333001, 333004, 333011, 333012, 333013, 333014, 333015, 333016, 333017, 333018, 333019, 333020, 333021, 333022) THEN 1
      ELSE 0
    END,
    `del_flag` = CASE
      WHEN `id` IN (333001, 333004, 333011, 333012, 333013, 333014, 333015, 333016, 333017, 333018, 333019, 333020, 333021, 333022) THEN 0
      ELSE `del_flag`
    END,
    `updated_at` = NOW()
WHERE `tenant_id` = 1;

UPDATE `payment_method_route_rule`
SET `status` = CASE
      WHEN `id` IN (334001, 334003, 334004, 334005, 334006, 334007, 334008, 334009, 334010, 334011, 334012, 334013) THEN 1
      ELSE 0
    END,
    `del_flag` = CASE
      WHEN `id` IN (334001, 334003, 334004, 334005, 334006, 334007, 334008, 334009, 334010, 334011, 334012, 334013) THEN 0
      ELSE 1
    END,
    `updated_at` = NOW()
WHERE `tenant_id` = 1;

UPDATE `payment_method_route_rule_item`
SET `status` = CASE
      WHEN `id` IN (335001, 335003, 335004, 335005, 335006, 335007, 335008, 335009, 335010, 335011, 335012, 335013) THEN 1
      ELSE 0
    END,
    `del_flag` = CASE
      WHEN `id` IN (335001, 335003, 335004, 335005, 335006, 335007, 335008, 335009, 335010, 335011, 335012, 335013) THEN 0
      ELSE `del_flag`
    END,
    `updated_at` = NOW()
WHERE `tenant_id` = 1;

INSERT INTO `payment_method_route_rule`
  (`id`, `rule_code`, `rule_name`, `app_id`, `subject_id`, `method_code`, `terminal_type`, `environment`, `route_mode`, `fallback_enabled`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (334008, 'MEMBER_CENTER_WECHAT_QR_MANGO_PAY', '会员中心微信扫码芒果支付路由', 310002, 320002, 'PERSONAL_WECHAT_QR', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (334009, 'MEMBER_CENTER_ALIPAY_QR_MANGO_PAY', '会员中心支付宝扫码芒果支付路由', 310002, 320002, 'PERSONAL_ALIPAY_QR', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (334010, 'MEMBER_CENTER_ALIPAY_PC_MANGO_PAY', '会员中心支付宝账户支付芒果支付路由', 310002, 320002, 'PERSONAL_ALIPAY_PC', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (334011, 'MEMBER_CENTER_PERSONAL_EBANK_MANGO_PAY', '会员中心个人网银芒果支付路由', 310002, 320002, 'PERSONAL_EBANK_REDIRECT', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (334012, 'MEMBER_CENTER_CORPORATE_EBANK_MANGO_PAY', '会员中心企业网银芒果支付路由', 310002, 320002, 'CORPORATE_EBANK_REDIRECT', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (334013, 'MEMBER_CENTER_OFFLINE_COLLECTION', '会员中心线下转账线下收款通道路由', 310002, 320002, 'CORPORATE_OFFLINE_ACCOUNT', 'WEB', 'OFFLINE_COLLECTION', 'PRIORITY', 0, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `rule_name` = VALUES(`rule_name`),
  `app_id` = VALUES(`app_id`),
  `subject_id` = VALUES(`subject_id`),
  `method_code` = VALUES(`method_code`),
  `terminal_type` = VALUES(`terminal_type`),
  `environment` = VALUES(`environment`),
  `route_mode` = VALUES(`route_mode`),
  `fallback_enabled` = VALUES(`fallback_enabled`),
  `status` = 1,
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_method_route_rule_item`
  (`id`, `rule_id`, `contract_capability_id`, `priority`, `weight`, `min_amount`, `max_amount`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (335008, 334008, 333016, 10, 100, 1, 5000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (335009, 334009, 333017, 10, 100, 1, 5000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (335010, 334010, 333018, 10, 100, 1, 5000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (335011, 334011, 333019, 10, 100, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (335012, 334012, 333020, 10, 100, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (335013, 334013, 333022, 10, 100, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `contract_capability_id` = VALUES(`contract_capability_id`),
  `priority` = VALUES(`priority`),
  `weight` = VALUES(`weight`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `status` = 1,
  `del_flag` = 0,
  `updated_at` = NOW();

UPDATE `payment_channel_contract_capability`
SET `del_flag` = 1,
    `status` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `contract_id` IN (331001, 331002, 331003, 331004, 331005, 331006, 331007, 331008)
  AND `id` NOT IN (333001, 333004, 333011, 333012, 333013, 333014, 333015, 333016, 333017, 333018, 333019, 333020, 333021, 333022)
  AND `del_flag` = 0;

UPDATE `payment_method_route_rule_item`
SET `del_flag` = 1,
    `status` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `rule_id` IN (334001, 334002, 334003, 334004, 334005, 334006, 334007, 334008, 334009, 334010, 334011, 334012, 334013)
  AND `id` NOT IN (335001, 335003, 335004, 335005, 335006, 335007, 335008, 335009, 335010, 335011, 335012, 335013)
  AND `del_flag` = 0;
