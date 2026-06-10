UPDATE `payment_channel_contract` `contract`
JOIN `payment_enterprise_subject` `subject`
  ON `subject`.`id` = `contract`.`subject_id`
 AND `subject`.`tenant_id` = `contract`.`tenant_id`
 AND `subject`.`del_flag` = 0
SET `contract`.`config_values_json` = JSON_OBJECT(
        'accountName', `subject`.`subject_name`,
        'accountNo', `subject`.`bank_account_no`,
        'bankName', `subject`.`bank_name`,
        'voucherRequired', true,
        'reconciliationCodeRequired', true
    ),
    `contract`.`updated_at` = NOW()
WHERE `contract`.`id` = 331004
  AND `contract`.`tenant_id` = 1
  AND `contract`.`channel_id` = 330004
  AND `contract`.`del_flag` = 0;

UPDATE `payment_channel_contract` `contract`
JOIN `payment_enterprise_subject` `subject`
  ON `subject`.`id` = `contract`.`subject_id`
 AND `subject`.`tenant_id` = `contract`.`tenant_id`
 AND `subject`.`del_flag` = 0
SET `contract`.`config_values_json` = JSON_OBJECT(
        'accountName', `subject`.`subject_name`,
        'accountNo', `subject`.`bank_account_no`,
        'bankName', `subject`.`bank_name`,
        'voucherRequired', true,
        'reconciliationCodeRequired', true
    ),
    `contract`.`updated_at` = NOW()
WHERE `contract`.`id` = 331008
  AND `contract`.`tenant_id` = 1
  AND `contract`.`channel_id` = 330004
  AND `contract`.`del_flag` = 0;
