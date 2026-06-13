UPDATE `payment_channel`
SET `field_template_json` = '[
  {"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true,"sort":1,"group":"基础信息"},
  {"name":"apiSecret","label":"接口密钥","component":"password","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":2,"group":"安全凭证"},
  {"name":"certificateFileId","label":"商户证书文件","component":"fileId","dataType":"fileId","required":true,"sort":3,"group":"安全凭证"},
  {"name":"mangoPayScenario","label":"芒果支付场景","component":"select","dataType":"string","required":false,"sort":4,"group":"场景控制","options":[{"label":"支付成功","value":"SUCCESS"},{"label":"支付处理中","value":"PROCESSING"},{"label":"支付失败","value":"FAIL"}]}
]',
    `update_time` = NOW()
WHERE `id` = 330001
  AND `channel_code` = 'MANGO_PAY';
