UPDATE `payment_channel`
SET `channel_name` = '芒果支付',
    `channel_type` = 'BUILTIN_VIRTUAL',
    `adapter_type` = 'MANGO_PAY',
    `update_time` = NOW()
WHERE `channel_code` = 'MANGO_PAY';

UPDATE `payment_channel_contract`
SET `contract_name` = '芒果科技芒果支付签约',
    `update_time` = NOW()
WHERE `contract_code` = 'MANGO_PAY_MANGO_TECH';
