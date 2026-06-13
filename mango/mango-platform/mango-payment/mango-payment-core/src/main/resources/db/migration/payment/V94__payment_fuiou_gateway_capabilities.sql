UPDATE `payment_channel`
SET
  `gateway_url` = 'https://pay.fuioupay.com/smpGate.do',
  `gateway_base_url` = 'https://fundwx.payfuiouo2o.com',
  `field_template_json` = '[
      {"name":"scanpayGatewayBaseUrl","label":"扫码接口地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":1,"group":"扫码支付","placeholder":"https://fundwx.payfuiouo2o.com"},
      {"name":"insCd","label":"机构号","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":2,"group":"扫码支付"},
      {"name":"merchantNo","label":"扫码商户号","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":3,"group":"扫码支付"},
      {"name":"operatorId","label":"退款操作员","component":"input","dataType":"string","required":false,"sensitive":false,"encrypted":false,"masked":false,"sort":4,"group":"扫码支付"},
      {"name":"notifyUrl","label":"扫码通知地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":5,"group":"扫码支付"},
      {"name":"privateKey","label":"商户私钥","component":"textarea","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":6,"group":"扫码支付","description":"富友开户资料提供的商户私钥，保存后加密并脱敏展示"},
      {"name":"fuiouPublicKey","label":"富友平台公钥","component":"textarea","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":7,"group":"扫码支付","description":"富友开户资料提供的平台公钥"},
      {"name":"gatewayMerchantNo","label":"网关商户号","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":20,"group":"网银支付"},
      {"name":"gatewayMerchantKey","label":"网关商户密钥","component":"password","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":21,"group":"网银支付","description":"富友支付网关开户资料提供的商户密钥"},
      {"name":"gatewayPayUrl","label":"网关支付地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":22,"group":"网银支付","placeholder":"https://pay.fuioupay.com/smpGate.do"},
      {"name":"gatewayQueryUrl","label":"网关查单地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":23,"group":"网银支付","placeholder":"https://pay.fuioupay.com/smpAQueryGate.do"},
      {"name":"gatewayPageNotifyUrl","label":"页面跳转地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":24,"group":"网银支付"},
      {"name":"gatewayBackNotifyUrl","label":"后台通知地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":25,"group":"网银支付"}
    ]',
  `capability_summary` = '富友支付通道：扫码支付已接入微信扫码、支付宝扫码、查单和退款；网银支付已接入个人网银、企业网银发起支付和查单。扫码商户资料与网关商户资料在签约通道动态维护；通道内部参数由系统适配器处理，不作为商户维护项。网银退款和富友账单接口资料未确认前不开放对应能力。',
  `bill_fetch_modes` = NULL,
  `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `channel_code` = 'FUIOU_PAY'
  AND `del_flag` = 0;

INSERT INTO `payment_channel_capability`
  (`id`, `channel_id`, `method_code`, `terminal_type`, `environment`, `supports_refund`, `supports_query`, `supports_close`, `supports_bill`, `supports_reconcile`, `min_amount`, `max_amount`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (332016, 330005, 'PERSONAL_ALIPAY_QR', 'WEB', 'PROD', 1, 1, 1, 0, 0, 1, 1000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (332017, 330005, 'PERSONAL_WECHAT_QR', 'WEB', 'PROD', 1, 1, 1, 0, 0, 1, 1000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (332018, 330005, 'PERSONAL_EBANK_REDIRECT', 'WEB', 'PROD', 0, 1, 0, 0, 0, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (332019, 330005, 'CORPORATE_EBANK_REDIRECT', 'WEB', 'PROD', 0, 1, 0, 0, 0, 1, 20000000, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `method_code` = VALUES(`method_code`),
  `terminal_type` = VALUES(`terminal_type`),
  `environment` = VALUES(`environment`),
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

UPDATE `payment_channel_contract`
SET
  `merchant_no` = '0002900F0370542',
  `config_values_json` = JSON_MERGE_PATCH(
    JSON_OBJECT(
      'scanpayGatewayBaseUrl', 'https://fundwx.payfuiouo2o.com',
      'insCd', '08A9999999',
      'merchantNo', '0002900F0370542',
      'notifyUrl', 'https://payment.example.com/api/payment/channel-callbacks/fuiou',
      'operatorId', 'mango',
      'fuiouPublicKey', 'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDz2fCOYaaU6sztFql4cOmiFRq2LRk1XuGfrJnMFa09QMXMXOEn9YNYC44zV1AE/q9b0BKGbM74YPoge/7qsW+Heao76Drv6HujP+rXLFbsXT5f9rcID2GCzDc+DXjb+NfwSa8vS9KJ3dau2xm87zpjdQ9zER6VH4UcZTgj7LbzgwIDAQAB',
      'gatewayMerchantNo', '0001000F0040992',
      'gatewayMerchantKey', 'vau6p7ldawpezyaugc0kopdrrwm4gkpu',
      'gatewayPayUrl', 'http://www-2.wg.fuiou.com:13195/smpGate.do',
      'gatewayQueryUrl', 'http://www-2.wg.fuiou.com:13195/smpAQueryGate.do',
      'gatewayPageNotifyUrl', 'https://payment.example.com/payment/fuiou/return',
      'gatewayBackNotifyUrl', 'https://payment.example.com/api/payment/channel-callbacks/fuiou'
    ),
    JSON_REMOVE(
      COALESCE(`config_values_json`, JSON_OBJECT()),
      '$.gatewayBaseUrl',
      '$.termId',
      '$.termIp',
      '$.interfaceMode',
      '$.xmlRsa'
    )
  ),
  `enabled_method_codes` = 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT',
  `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `contract_code` = 'FUIOU_PAY_MANGO_TECH'
  AND `del_flag` = 0;

INSERT INTO `payment_channel_contract_capability`
  (`id`, `contract_id`, `channel_capability_id`, `method_code`, `terminal_type`, `fee_rate`, `min_amount`, `max_amount`, `priority`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (333023, 331009, 332017, 'PERSONAL_WECHAT_QR', 'WEB', 0.0020000000, 1, 1000000, 30, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333024, 331009, 332016, 'PERSONAL_ALIPAY_QR', 'WEB', 0.0020000000, 1, 1000000, 31, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333025, 331009, 332018, 'PERSONAL_EBANK_REDIRECT', 'WEB', 0.0020000000, 1, 20000000, 32, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333026, 331009, 332019, 'CORPORATE_EBANK_REDIRECT', 'WEB', 0.0020000000, 1, 20000000, 33, 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `channel_capability_id` = VALUES(`channel_capability_id`),
  `method_code` = VALUES(`method_code`),
  `terminal_type` = VALUES(`terminal_type`),
  `fee_rate` = VALUES(`fee_rate`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `priority` = VALUES(`priority`),
  `status` = 1,
  `del_flag` = 0,
  `updated_at` = NOW();
