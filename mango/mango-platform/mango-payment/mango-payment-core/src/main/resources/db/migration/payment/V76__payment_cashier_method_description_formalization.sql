UPDATE `payment_method`
SET `description` = '微信客户端扫码完成支付'
WHERE `method_code` = 'PERSONAL_WECHAT_QR'
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `description` = '支付宝客户端扫码完成支付'
WHERE `method_code` = 'PERSONAL_ALIPAY_QR'
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `description` = '支付宝账户支付'
WHERE `method_code` = 'PERSONAL_ALIPAY_PC'
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `description` = '选择银行后进入个人网银支付'
WHERE `method_code` = 'PERSONAL_EBANK_REDIRECT'
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `description` = '选择银行后进入企业网银支付'
WHERE `method_code` = 'CORPORATE_EBANK_REDIRECT'
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `description` = '获取收款账户和转账备注，转账后提交凭证'
WHERE `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `del_flag` = 0;
