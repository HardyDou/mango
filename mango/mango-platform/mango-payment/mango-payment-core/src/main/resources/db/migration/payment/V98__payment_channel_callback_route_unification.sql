UPDATE `payment_channel_contract`
SET `config_values_json` = REPLACE(`config_values_json`,
    '/api/payment/channel-callbacks/fuiou',
    '/api/payment/channel-callbacks/fuiou_pay')
WHERE `channel_id` IN (
    SELECT `id`
    FROM `payment_channel`
    WHERE `channel_code` = 'FUIOU_PAY'
)
  AND `config_values_json` LIKE '%/api/payment/channel-callbacks/fuiou%';
