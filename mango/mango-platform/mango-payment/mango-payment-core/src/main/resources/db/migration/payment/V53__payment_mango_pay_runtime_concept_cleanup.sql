UPDATE `payment_channel_contract`
SET `environment` = 'MANGO_PAY',
    `merchant_no` = 'MANGO_PAY_MERCHANT_001',
    `contract_name` = '芒果科技芒果支付签约',
    `updated_at` = NOW()
WHERE `contract_code` = 'MANGO_PAY_MANGO_TECH'
  AND `del_flag` = 0;

UPDATE `payment_channel_capability`
SET `environment` = 'MANGO_PAY',
    `updated_at` = NOW()
WHERE `channel_id` = 330001;

UPDATE `payment_method_route_rule`
SET `environment` = 'MANGO_PAY',
    `updated_at` = NOW()
WHERE `id` IN (334001, 334002)
  AND `del_flag` = 0;
