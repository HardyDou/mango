INSERT INTO `payment_channel`
  (`id`, `channel_code`, `channel_name`, `environment`, `channel_type`, `adapter_type`, `merchant_no`, `gateway_url`, `public_key_ref`, `private_key_ref`, `gateway_base_url`, `field_template_json`, `capability_summary`, `bill_fetch_modes`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (330005, 'FUIOU_PAY', '富友支付', 'PROD', 'AGGREGATOR', 'FUIOU_PAY', '0002900F0370542', 'https://fundwx.payfuiouo2o.com', NULL, NULL, 'https://fundwx.payfuiouo2o.com',
   '[
      {"name":"gatewayBaseUrl","label":"网关地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":1,"group":"基础接入","placeholder":"https://fundwx.payfuiouo2o.com"},
      {"name":"insCd","label":"机构号","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":2,"group":"基础接入"},
      {"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":3,"group":"基础接入"},
      {"name":"operatorId","label":"退款操作员","component":"input","dataType":"string","required":false,"sensitive":false,"encrypted":false,"masked":false,"sort":5,"group":"基础接入"},
      {"name":"notifyUrl","label":"异步通知地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":10,"group":"回调地址"},
      {"name":"privateKey","label":"商户 RSA 私钥","component":"textarea","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":20,"group":"密钥证书","description":"富友扫码 XML/RSA 签名使用，保存后加密并脱敏展示"},
      {"name":"fuiouPublicKey","label":"富友 RSA 公钥","component":"textarea","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":21,"group":"密钥证书","description":"富友扫码 XML/RSA 响应验签使用"}
    ]',
   '富友支付通道：当前已接入扫码 XML/RSA 主扫统一下单，支持微信扫码、支付宝扫码、查单、退款；商户号、机构号、RSA 密钥和通知地址在签约通道动态维护。富友报文终端号由系统按线上收款接口规则处理，不作为商户签约维护项；被扫、支付宝账号支付、网银等能力需完成对应适配器后再开放；富友账单获取协议未完成适配前不声明通道账单能力。',
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
  (332016, 330005, 'PERSONAL_ALIPAY_QR', 'WEB', 'PROD', 1, 1, 0, 0, 0, 1, 1000000, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (332017, 330005, 'PERSONAL_WECHAT_QR', 'WEB', 'PROD', 1, 1, 0, 0, 0, 1, 1000000, 1, 1, NULL, NOW(), NULL, NOW(), 0)
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

INSERT INTO `payment_channel_contract`
  (`id`, `contract_code`, `contract_name`, `subject_id`, `channel_id`, `environment`, `merchant_no`, `app_id`, `config_values_json`, `enabled_method_codes`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (331009, 'FUIOU_PAY_MANGO_TECH', '芒果科技富友支付签约', 320001, 330005, 'PROD', '0002900F0370542', 'fuiou-pay-mango-tech',
   JSON_OBJECT(
     'insCd', '08A9999999',
     'merchantNo', '0002900F0370542',
     'gatewayBaseUrl', 'https://fundwx.payfuiouo2o.com',
     'notifyUrl', 'https://payment.example.com/api/payment/channel-callbacks/fuiou',
     'operatorId', 'mango',
     'fuiouPublicKey', 'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDz2fCOYaaU6sztFql4cOmiFRq2LRk1XuGfrJnMFa09QMXMXOEn9YNYC44zV1AE/q9b0BKGbM74YPoge/7qsW+Heao76Drv6HujP+rXLFbsXT5f9rcID2GCzDc+DXjb+NfwSa8vS9KJ3dau2xm87zpjdQ9zER6VH4UcZTgj7LbzgwIDAQAB'
   ),
   'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR', 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `contract_name` = VALUES(`contract_name`),
  `subject_id` = VALUES(`subject_id`),
  `channel_id` = VALUES(`channel_id`),
  `environment` = VALUES(`environment`),
  `merchant_no` = VALUES(`merchant_no`),
  `app_id` = VALUES(`app_id`),
  `config_values_json` = JSON_REMOVE(
      JSON_MERGE_PATCH(
        VALUES(`config_values_json`),
        COALESCE(`config_values_json`, JSON_OBJECT())
      ),
      '$.termId',
      '$.termIp',
      '$.interfaceMode',
      '$.mchntKey',
      '$.merchantCertFileId',
      '$.fuiouPublicKeyFileId',
      '$.wechatSubAppId',
      '$.alipayAppId',
      '$.ebankProductCode',
      '$.supportedBankCodes'
    ),
  `enabled_method_codes` = VALUES(`enabled_method_codes`),
  `status` = 1,
  `del_flag` = 0,
  `updated_at` = NOW();

INSERT INTO `payment_channel_contract_capability`
  (`id`, `contract_id`, `channel_capability_id`, `method_code`, `terminal_type`, `fee_rate`, `min_amount`, `max_amount`, `priority`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (333023, 331009, 332017, 'PERSONAL_WECHAT_QR', 'WEB', 0.0020000000, 1, 1000000, 30, 1, 1, NULL, NOW(), NULL, NOW(), 0),
  (333024, 331009, 332016, 'PERSONAL_ALIPAY_QR', 'WEB', 0.0020000000, 1, 1000000, 31, 1, 1, NULL, NOW(), NULL, NOW(), 0)
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

DELETE FROM `payment_channel_contract_capability`
WHERE `tenant_id` = 1
  AND `contract_id` = 331009
  AND `id` NOT IN (333023, 333024);

DELETE FROM `payment_channel_capability`
WHERE `tenant_id` = 1
  AND `channel_id` = 330005
  AND `id` NOT IN (332016, 332017);
