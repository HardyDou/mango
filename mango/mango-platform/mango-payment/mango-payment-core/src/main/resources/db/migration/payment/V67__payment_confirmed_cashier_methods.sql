UPDATE `payment_channel_contract`
SET `enabled_method_codes` = 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_QR,PERSONAL_ALIPAY_PC,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT',
    `updated_at` = NOW()
WHERE `id` = 331001
  AND `del_flag` = 0;

UPDATE `payment_channel_capability`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `channel_id` = 330001
  AND `method_code` IN (
    'PERSONAL_ALIPAY_H5',
    'PERSONAL_UNIONPAY_QR',
    'PERSONAL_DEBIT_QUICK',
    'PERSONAL_CREDIT_QUICK',
    'PERSONAL_WALLET_QUICK',
    'CORPORATE_OFFLINE_ACCOUNT'
  )
  AND `del_flag` = 0;

UPDATE `payment_channel_contract_capability`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `contract_id` = 331001
  AND `method_code` IN (
    'PERSONAL_ALIPAY_H5',
    'PERSONAL_UNIONPAY_QR',
    'PERSONAL_DEBIT_QUICK',
    'PERSONAL_CREDIT_QUICK',
    'PERSONAL_WALLET_QUICK',
    'CORPORATE_OFFLINE_ACCOUNT'
  )
  AND `del_flag` = 0;

UPDATE `payment_method_route_rule`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `method_code` IN (
    'PERSONAL_ALIPAY_H5',
    'PERSONAL_UNIONPAY_QR',
    'PERSONAL_DEBIT_QUICK',
    'PERSONAL_CREDIT_QUICK',
    'PERSONAL_WALLET_QUICK'
  )
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `method_code` IN (
    'PERSONAL_ALIPAY_H5',
    'PERSONAL_UNIONPAY_QR',
    'PERSONAL_DEBIT_QUICK',
    'PERSONAL_CREDIT_QUICK',
    'PERSONAL_WALLET_QUICK'
  )
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `status` = 1,
    `updated_at` = NOW()
WHERE `method_code` IN (
    'PERSONAL_ALIPAY_PC',
    'PERSONAL_ALIPAY_QR',
    'PERSONAL_WECHAT_QR',
    'PERSONAL_EBANK_REDIRECT',
    'CORPORATE_EBANK_REDIRECT',
    'CORPORATE_OFFLINE_ACCOUNT'
  )
  AND `del_flag` = 0;

UPDATE `payment_cashier_config`
SET `method_codes` = 'PERSONAL_ALIPAY_PC,PERSONAL_ALIPAY_QR,PERSONAL_WECHAT_QR,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `method_display_order` = 'PERSONAL_ALIPAY_PC,PERSONAL_ALIPAY_QR,PERSONAL_WECHAT_QR,PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT',
    `default_method_code` = 'PERSONAL_WECHAT_QR',
    `updated_at` = NOW()
WHERE `id` IN (350001, 350002)
  AND `del_flag` = 0;
