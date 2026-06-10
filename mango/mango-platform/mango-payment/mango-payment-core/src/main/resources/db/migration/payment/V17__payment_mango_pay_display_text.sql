UPDATE `payment_channel`
SET `channel_name` = '芒果支付',
    `update_time` = NOW()
WHERE `id` = 330001
  AND `channel_code` = 'MANGO_PAY';

UPDATE `payment_channel_contract`
SET `contract_name` = '芒果科技芒果支付签约',
    `updated_at` = NOW()
WHERE `id` = 331001
  AND `contract_code` = 'MANGO_PAY_MANGO_TECH';
