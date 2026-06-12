UPDATE `payment_channel`
SET
  `field_template_json` = '[
      {"name":"gatewayBaseUrl","label":"网关地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":1,"group":"基础接入","placeholder":"https://fundwx.payfuiouo2o.com"},
      {"name":"insCd","label":"机构号","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":2,"group":"基础接入"},
      {"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":3,"group":"基础接入"},
      {"name":"operatorId","label":"退款操作员","component":"input","dataType":"string","required":false,"sensitive":false,"encrypted":false,"masked":false,"sort":5,"group":"基础接入"},
      {"name":"notifyUrl","label":"异步通知地址","component":"url","dataType":"url","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":10,"group":"回调地址"},
      {"name":"privateKey","label":"商户私钥","component":"textarea","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":20,"group":"密钥证书","description":"富友开户资料提供的商户私钥，保存后加密并脱敏展示"},
      {"name":"fuiouPublicKey","label":"富友平台公钥","component":"textarea","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":21,"group":"密钥证书","description":"富友开户资料提供的平台公钥"}
    ]',
  `capability_summary` = '富友支付通道：当前已接入微信扫码、支付宝扫码、查单、退款；商户号、机构号、商户私钥、富友平台公钥和通知地址在签约通道动态维护。富友接口内部参数由系统按线上收款接口规则处理，不作为商户签约维护项；被扫、支付宝账号支付、网银等能力需完成对应适配器后再开放。',
  `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `channel_code` = 'FUIOU_PAY'
  AND `del_flag` = 0;
