UPDATE `payment_channel`
SET `channel_name` = '芒果支付',
    `capability_summary` = '芒果支付内置虚拟通道，支持全部标准支付方式、支付下单、回调、查单、关单、退款、退款查询、账单、对账、返回码映射和异常场景控制',
    `updated_at` = NOW()
WHERE `channel_code` = 'MANGO_PAY'
  AND `del_flag` = 0
  AND (
    `channel_name` <> '芒果支付'
    OR `capability_summary` <> '芒果支付内置虚拟通道，支持全部标准支付方式、支付下单、回调、查单、关单、退款、退款查询、账单、对账、返回码映射和异常场景控制'
  );

UPDATE `payment_channel_contract`
SET `contract_name` = '芒果科技芒果支付签约',
    `updated_at` = NOW()
WHERE `contract_code` = 'MANGO_PAY_MANGO_TECH'
  AND `del_flag` = 0
  AND `contract_name` <> '芒果科技芒果支付签约';

UPDATE `payment_channel_contract`
SET `contract_name` = REPLACE(REPLACE(REPLACE(REPLACE(`contract_name`,
        CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
        '芒果支付芒果支付', '芒果支付'),
        '自建芒果支付通道', '芒果支付'),
        '自建芒果支付', '芒果支付'),
    `updated_at` = NOW()
WHERE `del_flag` = 0
  AND (
    `contract_name` LIKE CONCAT('%', CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '%')
    OR `contract_name` LIKE '%芒果支付芒果支付%'
    OR `contract_name` LIKE '%自建芒果支付%'
  );

UPDATE `payment_method_route_rule`
SET `rule_name` = REPLACE(REPLACE(REPLACE(REPLACE(`rule_name`,
        CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
        '芒果支付芒果支付', '芒果支付'),
        '自建芒果支付通道', '芒果支付'),
        '自建芒果支付', '芒果支付'),
    `updated_at` = NOW()
WHERE `del_flag` = 0
  AND (
    `rule_name` LIKE CONCAT('%', CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '%')
    OR `rule_name` LIKE '%芒果支付芒果支付%'
    OR `rule_name` LIKE '%自建芒果支付%'
  );

UPDATE `payment_cashier_config`
SET `display_config` = REPLACE(REPLACE(REPLACE(REPLACE(`display_config`,
        CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
        '芒果支付芒果支付', '芒果支付'),
        '自建芒果支付通道', '芒果支付'),
        '自建芒果支付', '芒果支付'),
    `updated_at` = NOW()
WHERE `del_flag` = 0
  AND (
    `display_config` LIKE CONCAT('%', CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '%')
    OR `display_config` LIKE '%芒果支付芒果支付%'
    OR `display_config` LIKE '%自建芒果支付%'
  );
