UPDATE `payment_channel`
SET `environment` = CASE
      WHEN `channel_code` = 'MANGO_PAY' THEN 'MANGO_PAY'
      WHEN `channel_code` = 'OFFLINE_COLLECTION' THEN 'OFFLINE_COLLECTION'
      ELSE 'PROD'
    END,
    `updated_at` = NOW()
WHERE `del_flag` = 0;

UPDATE `payment_channel_contract` `contract`
JOIN `payment_channel` `channel`
  ON `channel`.`id` = `contract`.`channel_id`
 AND `channel`.`tenant_id` = `contract`.`tenant_id`
 AND `channel`.`del_flag` = 0
SET `contract`.`environment` = CASE
      WHEN `channel`.`channel_code` = 'MANGO_PAY' THEN 'MANGO_PAY'
      WHEN `channel`.`channel_code` = 'OFFLINE_COLLECTION' THEN 'OFFLINE_COLLECTION'
      ELSE 'PROD'
    END,
    `contract`.`updated_at` = NOW()
WHERE `contract`.`del_flag` = 0;

UPDATE `payment_channel_capability` `capability`
JOIN `payment_channel` `channel`
  ON `channel`.`id` = `capability`.`channel_id`
 AND `channel`.`tenant_id` = `capability`.`tenant_id`
 AND `channel`.`del_flag` = 0
SET `capability`.`environment` = CASE
      WHEN `channel`.`channel_code` = 'MANGO_PAY' THEN 'MANGO_PAY'
      WHEN `channel`.`channel_code` = 'OFFLINE_COLLECTION' THEN 'OFFLINE_COLLECTION'
      ELSE 'PROD'
    END,
    `capability`.`updated_at` = NOW()
WHERE `capability`.`del_flag` = 0;

UPDATE `payment_method_route_rule` `rule`
JOIN `payment_method_route_rule_item` `item`
  ON `item`.`rule_id` = `rule`.`id`
 AND `item`.`tenant_id` = `rule`.`tenant_id`
 AND `item`.`del_flag` = 0
JOIN `payment_channel_contract_capability` `contract_capability`
  ON `contract_capability`.`id` = `item`.`contract_capability_id`
 AND `contract_capability`.`tenant_id` = `rule`.`tenant_id`
 AND `contract_capability`.`del_flag` = 0
JOIN `payment_channel_contract` `contract`
  ON `contract`.`id` = `contract_capability`.`contract_id`
 AND `contract`.`tenant_id` = `rule`.`tenant_id`
 AND `contract`.`del_flag` = 0
SET `rule`.`environment` = `contract`.`environment`,
    `rule`.`updated_at` = NOW()
WHERE `rule`.`del_flag` = 0;
