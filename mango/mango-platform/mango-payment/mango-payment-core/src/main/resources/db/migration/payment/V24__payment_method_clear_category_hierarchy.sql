INSERT INTO `payment_method_category`
  (`id`, `category_code`, `category_name`, `level`, `parent_id`, `sort`, `status`, `tenant_id`, `created_by`, `updated_by`, `created_at`, `updated_at`, `del_flag`)
VALUES
  (360106, 'DEBIT_CARD', '储蓄卡', 2, 360001, 40, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360107, 'CREDIT_CARD', '信用卡', 2, 360001, 45, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360371, 'QUICK_PAY', '快捷支付', 3, 360106, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360372, 'QUICK_PAY', '快捷支付', 3, 360107, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `category_name` = VALUES(`category_name`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `updated_at` = NOW(),
  `del_flag` = 0;

UPDATE `payment_method_category`
SET `category_code` = 'QUICK_PAY',
    `category_name` = '快捷支付',
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `id` = 360341
  AND `parent_id` = 360105
  AND `level` = 3;

UPDATE `payment_method_category`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `id` IN (360104, 360331, 360332);

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'DEBIT_CARD',
    `interaction_type` = 'QUICK_PAY',
    `payment_material_type` = 'HTML_FORM',
    `requires_bank_selection` = 1,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `method_code` = 'PERSONAL_DEBIT_QUICK';

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'CREDIT_CARD',
    `interaction_type` = 'QUICK_PAY',
    `payment_material_type` = 'HTML_FORM',
    `requires_bank_selection` = 1,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `method_code` = 'PERSONAL_CREDIT_QUICK';

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'WALLET',
    `interaction_type` = 'QUICK_PAY',
    `payment_material_type` = 'H5_PARAM',
    `requires_bank_selection` = 0,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `method_code` = 'PERSONAL_WALLET_QUICK';
