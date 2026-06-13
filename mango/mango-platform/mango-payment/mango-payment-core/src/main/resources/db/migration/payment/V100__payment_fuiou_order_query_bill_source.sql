UPDATE `payment_channel`
SET
  `capability_summary` = '富友支付通道：扫码支付已接入微信扫码、支付宝扫码、查单、退款和退款查单；网银支付已接入个人网银、企业网银发起支付和查单。富友对账按当前文档采用逐笔查单模式：支付订单通过 commonQuery/网银查单确认，扫码退款通过 refundQuery 确认，并生成统一对账批次；如富友后续提供全量账单文件，可再增加文件型账单源。',
  `bill_fetch_modes` = 'HTTP',
  `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `channel_code` = 'FUIOU_PAY'
  AND `del_flag` = 0;

UPDATE `payment_channel_capability`
SET
  `supports_bill` = 1,
  `supports_reconcile` = 1,
  `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `channel_id` = 330005
  AND `method_code` IN ('PERSONAL_WECHAT_QR', 'PERSONAL_ALIPAY_QR', 'PERSONAL_EBANK_REDIRECT', 'CORPORATE_EBANK_REDIRECT')
  AND `del_flag` = 0;

INSERT INTO `payment_channel_bill_source`
  (`id`, `contract_id`, `channel_code`, `fetch_mode`, `endpoint`, `remote_path`, `credential_ref`, `page_mode`, `enabled`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)
VALUES
  (334005, 331009, 'FUIOU_PAY', 'HTTP', 'https://fundwx.payfuiouo2o.com', NULL, 'payment_channel_contract:331009', 'ORDER_QUERY', 1, 1, NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `channel_code` = VALUES(`channel_code`),
  `fetch_mode` = VALUES(`fetch_mode`),
  `endpoint` = VALUES(`endpoint`),
  `remote_path` = VALUES(`remote_path`),
  `credential_ref` = VALUES(`credential_ref`),
  `page_mode` = VALUES(`page_mode`),
  `enabled` = VALUES(`enabled`),
  `del_flag` = 0,
  `updated_at` = NOW();
