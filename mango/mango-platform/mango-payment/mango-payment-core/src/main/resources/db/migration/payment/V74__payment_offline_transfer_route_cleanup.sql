UPDATE `payment_channel_contract`
SET `enabled_method_codes` = TRIM(BOTH ',' FROM REPLACE(CONCAT(',', `enabled_method_codes`, ','), ',CORPORATE_OFFLINE_ACCOUNT,', ',')),
    `updated_at` = NOW()
WHERE `id` = 331001
  AND `del_flag` = 0;

UPDATE `payment_channel_capability`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `channel_id` = 330001
  AND `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `environment` = 'MANGO_PAY'
  AND `del_flag` = 0;

UPDATE `payment_channel_contract_capability`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `contract_id` = 331001
  AND `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `del_flag` = 0;

UPDATE `payment_method_route_rule`
SET `status` = 0,
    `fallback_enabled` = 0,
    `updated_at` = NOW()
WHERE `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `environment` = 'MANGO_PAY'
  AND `del_flag` = 0;

UPDATE `payment_method_route_rule_item` `item`
JOIN `payment_method_route_rule` `rule`
  ON `rule`.`id` = `item`.`rule_id`
 AND `rule`.`tenant_id` = `item`.`tenant_id`
 AND `rule`.`del_flag` = 0
SET `item`.`status` = 0,
    `item`.`updated_at` = NOW()
WHERE `rule`.`method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `rule`.`environment` = 'MANGO_PAY'
  AND `item`.`del_flag` = 0;

UPDATE `payment_channel_capability`
SET `status` = 1,
    `updated_at` = NOW()
WHERE `channel_id` = 330004
  AND `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `environment` = 'OFFLINE_COLLECTION'
  AND `del_flag` = 0;

UPDATE `payment_channel_contract_capability`
SET `status` = 1,
    `updated_at` = NOW()
WHERE `contract_id` = 331004
  AND `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `del_flag` = 0;

UPDATE `payment_method_route_rule`
SET `status` = 1,
    `fallback_enabled` = 0,
    `updated_at` = NOW()
WHERE `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `environment` = 'OFFLINE_COLLECTION'
  AND `del_flag` = 0;

UPDATE `payment_method_route_rule_item` `item`
JOIN `payment_method_route_rule` `rule`
  ON `rule`.`id` = `item`.`rule_id`
 AND `rule`.`tenant_id` = `item`.`tenant_id`
 AND `rule`.`del_flag` = 0
SET `item`.`status` = 1,
    `item`.`updated_at` = NOW()
WHERE `rule`.`method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `rule`.`environment` = 'OFFLINE_COLLECTION'
  AND `item`.`del_flag` = 0;
