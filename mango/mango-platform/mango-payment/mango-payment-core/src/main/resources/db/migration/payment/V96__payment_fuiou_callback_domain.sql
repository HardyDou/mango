UPDATE `payment_channel_contract` cc
JOIN `payment_channel` c
  ON c.id = cc.channel_id
SET cc.`config_values_json` = JSON_MERGE_PATCH(
      COALESCE(cc.`config_values_json`, JSON_OBJECT()),
      JSON_OBJECT(
        'notifyUrl', 'http://douxy.inner.yunxinbaokeji.com:7775/api/payment/channel-callbacks/fuiou',
        'gatewayPageNotifyUrl', 'http://douxy.inner.yunxinbaokeji.com:7775/api/payment/channel-callbacks/fuiou',
        'gatewayBackNotifyUrl', 'http://douxy.inner.yunxinbaokeji.com:7775/api/payment/channel-callbacks/fuiou'
      )
    ),
    cc.`updated_at` = NOW()
WHERE cc.`tenant_id` = 1
  AND c.`channel_code` = 'FUIOU_PAY'
  AND cc.`contract_code` = 'FUIOU_PAY_MANGO_TECH'
  AND cc.`del_flag` = 0;
