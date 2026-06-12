UPDATE `payment_channel_contract` cc
JOIN `payment_channel` c
  ON c.id = cc.channel_id
SET cc.`config_values_json` = JSON_MERGE_PATCH(
      COALESCE(cc.`config_values_json`, JSON_OBJECT()),
      JSON_OBJECT(
        'gatewayPageNotifyUrl', 'https://douxy.inner.yunxinbaokeji.com:1443/#/payment/gateway-result',
        'gatewayBackNotifyUrl', 'https://douxy.inner.yunxinbaokeji.com:1443/api/payment/channel-callbacks/fuiou_pay',
        'notifyUrl', 'https://douxy.inner.yunxinbaokeji.com:1443/api/payment/channel-callbacks/fuiou_pay'
      )
    ),
    cc.`updated_at` = NOW()
WHERE c.`channel_code` = 'FUIOU_PAY'
  AND cc.`del_flag` = 0;
