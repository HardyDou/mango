ALTER TABLE `payment_channel`
  MODIFY COLUMN `adapter_type` varchar(64) NOT NULL DEFAULT 'UNCONFIGURED' COMMENT '适配器类型';

UPDATE `payment_channel`
SET `channel_name` = '芒果支付',
    `channel_type` = 'BUILTIN_VIRTUAL',
    `adapter_type` = 'MANGO_PAY',
    `environment` = 'CHANNEL_PRODUCT',
    `merchant_no` = 'MANGO_PAY_MERCHANT_001',
    `gateway_base_url` = '/payment/mango-pay/virtual',
    `gateway_url` = '/payment/mango-pay/virtual',
    `field_template_json` = '[
  {"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":1,"group":"基础信息"},
  {"name":"apiSecret","label":"接口密钥","component":"password","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":2,"group":"安全凭证"},
  {"name":"certificateFileId","label":"商户证书文件","component":"fileId","dataType":"fileId","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":3,"group":"安全凭证"},
  {"name":"mangoPayScenario","label":"支付场景控制","component":"textarea","dataType":"string","required":false,"sensitive":false,"encrypted":false,"masked":false,"sort":4,"group":"场景控制"},
  {"name":"mangoPayRefundScenario","label":"退款场景控制","component":"textarea","dataType":"string","required":false,"sensitive":false,"encrypted":false,"masked":false,"sort":5,"group":"场景控制"}
]',
    `capability_summary` = '芒果支付内置虚拟通道，支持全部标准支付方式、支付下单、回调、查单、关单、退款、退款查询、账单、对账、返回码映射和异常场景控制',
    `updated_at` = NOW()
WHERE `channel_code` = 'MANGO_PAY'
  AND `del_flag` = 0;

UPDATE `payment_channel_contract`
SET `contract_name` = '芒果科技芒果支付签约',
    `environment` = 'MANGO_PAY',
    `merchant_no` = 'MANGO_PAY_MERCHANT_001',
    `app_id` = 'mango-pay-app',
    `updated_at` = NOW()
WHERE `contract_code` = 'MANGO_PAY_MANGO_TECH'
  AND `del_flag` = 0;

UPDATE `payment_channel_capability`
SET `environment` = 'MANGO_PAY',
    `updated_at` = NOW()
WHERE `channel_id` IN (
  SELECT `id`
  FROM `payment_channel`
  WHERE `channel_code` = 'MANGO_PAY'
);
