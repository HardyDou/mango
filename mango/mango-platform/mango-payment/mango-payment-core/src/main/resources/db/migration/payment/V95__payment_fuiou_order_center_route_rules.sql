UPDATE `payment_method_route_rule`
SET `rule_code` = 'ORDER_CENTER_WECHAT_QR_FUIOU_PAY',
    `rule_name` = '订单中心微信扫码富友支付路由',
    `environment` = 'PROD',
    `fallback_enabled` = 0,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `app_id` = 310001
  AND `subject_id` = 320001
  AND `method_code` = 'PERSONAL_WECHAT_QR'
  AND `terminal_type` = 'WEB';

UPDATE `payment_method_route_rule_item`
SET `contract_capability_id` = 333023,
    `priority` = 10,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `rule_id` = (
    SELECT `id`
    FROM `payment_method_route_rule`
    WHERE `tenant_id` = 1
      AND `app_id` = 310001
      AND `subject_id` = 320001
      AND `method_code` = 'PERSONAL_WECHAT_QR'
      AND `terminal_type` = 'WEB'
    LIMIT 1
  );

UPDATE `payment_method_route_rule`
SET `rule_code` = 'ORDER_CENTER_ALIPAY_QR_FUIOU_PAY',
    `rule_name` = '订单中心支付宝扫码富友支付路由',
    `environment` = 'PROD',
    `fallback_enabled` = 0,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `app_id` = 310001
  AND `subject_id` = 320001
  AND `method_code` = 'PERSONAL_ALIPAY_QR'
  AND `terminal_type` = 'WEB';

UPDATE `payment_method_route_rule_item`
SET `contract_capability_id` = 333024,
    `priority` = 10,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `rule_id` = (
    SELECT `id`
    FROM `payment_method_route_rule`
    WHERE `tenant_id` = 1
      AND `app_id` = 310001
      AND `subject_id` = 320001
      AND `method_code` = 'PERSONAL_ALIPAY_QR'
      AND `terminal_type` = 'WEB'
    LIMIT 1
  );

UPDATE `payment_method_route_rule`
SET `rule_code` = 'ORDER_CENTER_PERSONAL_EBANK_FUIOU_PAY',
    `rule_name` = '订单中心个人网银富友支付路由',
    `environment` = 'PROD',
    `fallback_enabled` = 0,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `app_id` = 310001
  AND `subject_id` = 320001
  AND `method_code` = 'PERSONAL_EBANK_REDIRECT'
  AND `terminal_type` = 'WEB';

UPDATE `payment_method_route_rule_item`
SET `contract_capability_id` = 333025,
    `priority` = 10,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `rule_id` = (
    SELECT `id`
    FROM `payment_method_route_rule`
    WHERE `tenant_id` = 1
      AND `app_id` = 310001
      AND `subject_id` = 320001
      AND `method_code` = 'PERSONAL_EBANK_REDIRECT'
      AND `terminal_type` = 'WEB'
    LIMIT 1
  );

UPDATE `payment_method_route_rule`
SET `rule_code` = 'ORDER_CENTER_CORPORATE_EBANK_FUIOU_PAY',
    `rule_name` = '订单中心企业网银富友支付路由',
    `environment` = 'PROD',
    `fallback_enabled` = 0,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `app_id` = 310001
  AND `subject_id` = 320001
  AND `method_code` = 'CORPORATE_EBANK_REDIRECT'
  AND `terminal_type` = 'WEB';

UPDATE `payment_method_route_rule_item`
SET `contract_capability_id` = 333026,
    `priority` = 10,
    `status` = 1,
    `del_flag` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `rule_id` = (
    SELECT `id`
    FROM `payment_method_route_rule`
    WHERE `tenant_id` = 1
      AND `app_id` = 310001
      AND `subject_id` = 320001
      AND `method_code` = 'CORPORATE_EBANK_REDIRECT'
      AND `terminal_type` = 'WEB'
    LIMIT 1
  );
