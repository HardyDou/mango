UPDATE `payment_channel`
SET `field_template_json` = '[
  {"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":1,"group":"基础信息"},
  {"name":"apiSecret","label":"接口密钥","component":"password","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":2,"group":"安全凭证"},
  {"name":"certificateFileId","label":"商户证书文件","component":"fileId","dataType":"fileId","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":3,"group":"安全凭证"},
  {"name":"mangoPayScenario","label":"支付场景控制","component":"textarea","dataType":"string","required":false,"sensitive":false,"encrypted":false,"masked":false,"sort":4,"group":"场景控制"},
  {"name":"mangoPayRefundScenario","label":"退款场景控制","component":"textarea","dataType":"string","required":false,"sensitive":false,"encrypted":false,"masked":false,"sort":5,"group":"场景控制"}
]',
    `updated_at` = NOW()
WHERE `channel_code` = 'MANGO_PAY'
  AND `del_flag` = 0;
