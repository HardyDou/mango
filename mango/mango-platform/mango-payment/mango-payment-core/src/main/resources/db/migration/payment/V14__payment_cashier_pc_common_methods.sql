INSERT INTO `payment_method`
  (`id`, `method_code`, `method_name`, `channel_id`, `account_nature`, `instrument_type`, `interaction_type`, `terminal_scope`, `payment_material_type`, `description`, `route_strategy`, `min_amount`, `max_amount`, `sort`, `status`, `tenant_id`, `create_by`, `update_by`, `create_time`, `update_time`, `del_flag`)
VALUES
  (340005, 'PERSONAL_UNIONPAY_QR', '云闪付', NULL, 'PERSONAL', 'UNIONPAY', 'QR_CODE', 'WEB,H5', 'QR', '银联云闪付二维码支付', '按租户、应用、主体、终端、金额命中签约能力', 1, 5000000, 5, 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (340006, 'PERSONAL_DEBIT_QUICK', '储蓄卡快捷支付', NULL, 'DEBIT', 'BANK_CARD', 'WEB_REDIRECT', 'WEB,H5', 'HTML_FORM', '储蓄卡快捷支付，支持短信验证或银行页确认', '按租户、应用、主体、终端、金额命中签约能力', 1, 20000000, 6, 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (340007, 'PERSONAL_CREDIT_QUICK', '信用卡快捷支付', NULL, 'CREDIT', 'BANK_CARD', 'WEB_REDIRECT', 'WEB,H5', 'HTML_FORM', '信用卡快捷支付，支持信用卡付款', '按租户、应用、主体、终端、金额命中签约能力', 1, 20000000, 7, 1, 1, 'system', 'system', NOW(), NOW(), 0),
  (340008, 'PERSONAL_WALLET_QUICK', '快捷钱包支付', NULL, 'PERSONAL', 'WALLET', 'H5_REDIRECT', 'WEB,H5', 'H5_PARAM', '快捷账户钱包支付', '按租户、应用、主体、终端、金额命中签约能力', 1, 5000000, 8, 1, 1, 'system', 'system', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `method_name` = VALUES(`method_name`),
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
  `status` = VALUES(`status`),
  `update_by` = VALUES(`update_by`),
  `update_time` = NOW(),
  `del_flag` = 0;

UPDATE `payment_channel_contract`
SET `enabled_method_codes` = 'PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT,PERSONAL_ALIPAY_H5,CORPORATE_EBANK_REDIRECT,PERSONAL_UNIONPAY_QR,PERSONAL_DEBIT_QUICK,PERSONAL_CREDIT_QUICK,PERSONAL_WALLET_QUICK',
    `contract_name` = '芒果科技芒果支付签约',
    `update_time` = NOW()
WHERE `id` = 331001;

INSERT INTO `payment_channel_capability`
  (`id`, `channel_id`, `method_code`, `terminal_type`, `environment`, `min_amount`, `max_amount`, `tenant_id`)
VALUES
  (332007, 330001, 'PERSONAL_UNIONPAY_QR', 'WEB', 'MANGO_PAY', 1, 5000000, 1),
  (332008, 330001, 'PERSONAL_DEBIT_QUICK', 'WEB', 'MANGO_PAY', 1, 20000000, 1),
  (332009, 330001, 'PERSONAL_CREDIT_QUICK', 'WEB', 'MANGO_PAY', 1, 20000000, 1),
  (332010, 330001, 'PERSONAL_WALLET_QUICK', 'WEB', 'MANGO_PAY', 1, 5000000, 1)
ON DUPLICATE KEY UPDATE
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `status` = 1,
  `update_time` = NOW();

INSERT INTO `payment_channel_contract_capability`
  (`id`, `contract_id`, `channel_capability_id`, `method_code`, `terminal_type`, `min_amount`, `max_amount`, `priority`, `tenant_id`)
VALUES
  (333007, 331001, 332007, 'PERSONAL_UNIONPAY_QR', 'WEB', 1, 5000000, 15, 1),
  (333008, 331001, 332008, 'PERSONAL_DEBIT_QUICK', 'WEB', 1, 20000000, 16, 1),
  (333009, 331001, 332009, 'PERSONAL_CREDIT_QUICK', 'WEB', 1, 20000000, 17, 1),
  (333010, 331001, 332010, 'PERSONAL_WALLET_QUICK', 'WEB', 1, 5000000, 18, 1)
ON DUPLICATE KEY UPDATE
  `priority` = VALUES(`priority`),
  `status` = 1,
  `update_time` = NOW();

UPDATE `payment_cashier_config`
SET `method_codes` = 'PERSONAL_DEBIT_QUICK,PERSONAL_CREDIT_QUICK,PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_H5,PERSONAL_UNIONPAY_QR,PERSONAL_WALLET_QUICK,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `method_display_order` = 'PERSONAL_DEBIT_QUICK,PERSONAL_CREDIT_QUICK,PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_H5,PERSONAL_UNIONPAY_QR,PERSONAL_WALLET_QUICK,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `default_method_code` = 'PERSONAL_WECHAT_QR',
    `display_config` = CASE
      WHEN `display_config` IS NULL OR `display_config` = '' THEN '{"subtitle":"请确认订单金额后选择支付方式","helpText":"支付遇到问题请联系业务客服。"}'
      ELSE `display_config`
    END,
    `update_time` = NOW()
WHERE `id` = 350001;

UPDATE `payment_method`
SET `status` = 1,
    `update_by` = 'system',
    `update_time` = NOW(),
    `del_flag` = 0
WHERE `method_code` IN (
  'PERSONAL_DEBIT_QUICK',
  'PERSONAL_CREDIT_QUICK',
  'PERSONAL_WECHAT_QR',
  'PERSONAL_ALIPAY_H5',
  'PERSONAL_UNIONPAY_QR',
  'PERSONAL_WALLET_QUICK',
  'CORPORATE_EBANK_REDIRECT',
  'CORPORATE_OFFLINE_ACCOUNT'
);
