UPDATE `payment_channel_contract` cc
JOIN `payment_channel` c
  ON c.id = cc.channel_id
SET cc.`config_values_json` = JSON_MERGE_PATCH(
      COALESCE(cc.`config_values_json`, JSON_OBJECT()),
      JSON_OBJECT(
        'fuiouPublicKey', 'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBv9K+jiuHqXIehX81oyNSD2RfVn+KTPb7NRT5HDPFE35CjZJd7Fu40r0U2Cp7Eyhayv/mRS6ZqvBT/8tQqwpUExTQQBbdZjfk+efb9bF9a+uCnAg0RsuqxeJ2r/rRTsORzVLJy+4GKcv06/p6CcBc5BI1gqSKmyyNBlgfkxLYewIDAQAB'
      )
    ),
    cc.`updated_at` = NOW()
WHERE cc.`tenant_id` = 1
  AND c.`channel_code` = 'FUIOU_PAY'
  AND cc.`contract_code` = 'FUIOU_PAY_MANGO_TECH'
  AND cc.`del_flag` = 0;
