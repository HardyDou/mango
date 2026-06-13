UPDATE `payment_channel`
SET `channel_code` = 'MANGO_PAY',
    `channel_name` = '芒果支付',
    `channel_type` = 'BUILTIN_VIRTUAL',
    `adapter_type` = 'MANGO_PAY',
    `environment` = 'CHANNEL_PRODUCT',
    `merchant_no` = 'MANGO_PAY_MERCHANT_001',
    `gateway_base_url` = '/payment/mango-pay/virtual',
    `gateway_url` = '/payment/mango-pay/virtual',
    `field_template_json` = '[{"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true},{"name":"mangoPayScenario","label":"支付场景控制","component":"textarea","dataType":"string","required":false},{"name":"mangoPayRefundScenario","label":"退款场景控制","component":"textarea","dataType":"string","required":false}]',
    `capability_summary` = '芒果支付内置虚拟通道，支持全部标准支付方式、支付下单、回调、查单、关单、退款、退款查询、账单、对账、返回码映射和异常场景控制',
    `updated_at` = NOW()
WHERE `id` = 330001
  AND `del_flag` = 0;

UPDATE `payment_channel_contract`
SET `contract_code` = 'MANGO_PAY_MANGO_TECH',
    `contract_name` = '芒果科技芒果支付签约',
    `environment` = 'MANGO_PAY',
    `merchant_no` = 'MANGO_PAY_MERCHANT_001',
    `app_id` = 'mango-pay-app',
    `config_values_json` = JSON_SET(
      COALESCE(NULLIF(`config_values_json`, ''), '{}'),
      '$.mangoPayScenario',
      COALESCE(
        JSON_UNQUOTE(JSON_EXTRACT(COALESCE(NULLIF(`config_values_json`, ''), '{}'), '$.mangoPayScenario')),
        'SUCCESS'
      )
    ),
    `updated_at` = NOW()
WHERE `id` = 331001
  AND `del_flag` = 0;

UPDATE `payment_channel_capability`
SET `environment` = 'MANGO_PAY',
    `updated_at` = NOW()
WHERE `channel_id` = 330001;

UPDATE `payment_method_route_rule`
SET `environment` = 'MANGO_PAY',
    `updated_at` = NOW()
WHERE `id` IN (334001, 334002);
