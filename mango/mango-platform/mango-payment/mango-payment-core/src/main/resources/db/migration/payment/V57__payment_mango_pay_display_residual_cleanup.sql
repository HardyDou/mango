UPDATE `payment_channel`
SET `channel_name` = '芒果支付',
    `capability_summary` = REPLACE(REPLACE(REPLACE(REPLACE(`capability_summary`,
        CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
        '芒果支付芒果支付', '芒果支付'),
        '自建芒果支付通道', '芒果支付'),
        '自建芒果支付', '芒果支付'),
    `updated_at` = NOW()
WHERE `channel_code` = 'MANGO_PAY'
  AND `del_flag` = 0
  AND (
    `channel_name` <> '芒果支付'
    OR `capability_summary` LIKE CONCAT('%', CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '%')
    OR `capability_summary` LIKE '%芒果支付芒果支付%'
    OR `capability_summary` LIKE '%自建芒果支付%'
  );

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
SET `cashier_name` = REPLACE(REPLACE(REPLACE(REPLACE(`cashier_name`,
        CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
        '芒果支付芒果支付', '芒果支付'),
        '自建芒果支付通道', '芒果支付'),
        '自建芒果支付', '芒果支付'),
    `display_config` = REPLACE(REPLACE(REPLACE(REPLACE(`display_config`,
        CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
        '芒果支付芒果支付', '芒果支付'),
        '自建芒果支付通道', '芒果支付'),
        '自建芒果支付', '芒果支付'),
    `updated_at` = NOW()
WHERE `del_flag` = 0
  AND (
    `cashier_name` LIKE CONCAT('%', CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '%')
    OR `cashier_name` LIKE '%芒果支付芒果支付%'
    OR `cashier_name` LIKE '%自建芒果支付%'
    OR `display_config` LIKE CONCAT('%', CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '%')
    OR `display_config` LIKE '%芒果支付芒果支付%'
    OR `display_config` LIKE '%自建芒果支付%'
  );

UPDATE `payment_mango_pay_scenario_control`
SET `remark` = REPLACE(REPLACE(REPLACE(REPLACE(`remark`,
        CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '芒果支付'),
        '芒果支付芒果支付', '芒果支付'),
        '自建芒果支付通道', '芒果支付'),
        '自建芒果支付', '芒果支付'),
    `updated_at` = NOW()
WHERE `del_flag` = 0
  AND (
    `remark` LIKE CONCAT('%', CONCAT(CHAR(26126), CHAR(26126), '芒果支付'), '%')
    OR `remark` LIKE '%芒果支付芒果支付%'
    OR `remark` LIKE '%自建芒果支付%'
  );
