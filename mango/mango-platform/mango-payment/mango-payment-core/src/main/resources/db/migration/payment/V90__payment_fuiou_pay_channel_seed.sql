INSERT INTO `payment_channel`
  (`id`, `channel_code`, `channel_name`, `environment`, `channel_type`, `adapter_type`, `merchant_no`, `gateway_url`, `public_key_ref`, `private_key_ref`, `gateway_base_url`, `field_template_json`, `capability_summary`, `bill_fetch_modes`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (330005, 'FUIOU_PAY', '富友支付', 'PROD', 'AGGREGATOR', 'FUIOU_PAY', '0002900F0370542', 'https://fundwx.fuiou.com', NULL, NULL, 'https://fundwx.fuiou.com',
   '[
      {"field":"insCd","label":"机构号","required":true,"sensitive":false},
      {"field":"merchantNo","label":"商户号","required":true,"sensitive":false},
      {"field":"termId","label":"终端号","required":true,"sensitive":false},
      {"field":"gatewayBaseUrl","label":"网关地址","required":true,"sensitive":false},
      {"field":"notifyUrl","label":"异步通知地址","required":true,"sensitive":false},
      {"field":"privateKey","label":"商户私钥","required":true,"sensitive":true},
      {"field":"fuiouPublicKey","label":"富友公钥","required":true,"sensitive":false},
      {"field":"termIp","label":"终端 IP","required":true,"sensitive":false},
      {"field":"operatorId","label":"操作员","required":false,"sensitive":false}
    ]',
   '富友支付真实通道，当前已接入支付宝扫码、支付查单和退款申请；微信、网银等能力需按富友补充资料另行开通后配置；富友账单获取协议未完成适配前不声明通道账单能力',
   NULL, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `channel_name` = VALUES(`channel_name`),
  `environment` = VALUES(`environment`),
  `channel_type` = VALUES(`channel_type`),
  `adapter_type` = VALUES(`adapter_type`),
  `merchant_no` = VALUES(`merchant_no`),
  `gateway_url` = VALUES(`gateway_url`),
  `gateway_base_url` = VALUES(`gateway_base_url`),
  `field_template_json` = VALUES(`field_template_json`),
  `capability_summary` = VALUES(`capability_summary`),
  `bill_fetch_modes` = VALUES(`bill_fetch_modes`),
  `status` = 1,
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_channel_capability`
  (`id`, `channel_id`, `method_code`, `terminal_type`, `environment`, `supports_refund`, `supports_query`, `supports_close`, `supports_bill`, `supports_reconcile`, `min_amount`, `max_amount`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (332016, 330005, 'PERSONAL_ALIPAY_QR', 'WEB', 'PROD', 1, 1, 0, 0, 0, 1, 1000000, 1, 1, NULL, NOW(), NULL, NOW(), 0)
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

INSERT INTO `payment_channel_contract`
  (`id`, `contract_code`, `contract_name`, `subject_id`, `channel_id`, `environment`, `merchant_no`, `app_id`, `config_values_json`, `enabled_method_codes`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (331009, 'FUIOU_PAY_MANGO_TECH', '芒果科技富友支付签约', 320001, 330005, 'PROD', '0002900F0370542', 'fuiou-pay-mango-tech',
   JSON_OBJECT(
     'insCd', '08A9999999',
     'merchantNo', '0002900F0370542',
     'termId', '88888888',
     'gatewayBaseUrl', 'https://fundwx.fuiou.com',
     'notifyUrl', 'https://payment.example.com/api/payment/channel-callbacks/fuiou',
     'privateKey', '',
     'fuiouPublicKey', 'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBv9K+jiuHqXIehX81oyNSD2RfVn+KTPb7NRT5HDPFE35CjZJd7Fu40r0U2Cp7Eyhayv/mRS6ZqvBT/8tQqwpUExTQQBbdZjfk+efb9bF9a+uCnAg0RsuqxeJ2r/rRTsORzVLJy+4GKcv06/p6CcBc5BI1gqSKmyyNBlgfkxLYewIDAQAB',
     'termIp', '127.0.0.1',
     'operatorId', 'mango'
   ),
   'PERSONAL_ALIPAY_QR', 1, 1, NULL, NOW(), NULL, NOW(), 0)
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

INSERT INTO `payment_channel_contract_capability`
  (`id`, `contract_id`, `channel_capability_id`, `method_code`, `terminal_type`, `fee_rate`, `min_amount`, `max_amount`, `priority`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (333023, 331009, 332016, 'PERSONAL_ALIPAY_QR', 'WEB', 0.0020000000, 1, 1000000, 30, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `fee_rate` = VALUES(`fee_rate`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `priority` = VALUES(`priority`),
  `status` = 1,
  `del_flag` = 0,
  `updated_at` = NOW();
