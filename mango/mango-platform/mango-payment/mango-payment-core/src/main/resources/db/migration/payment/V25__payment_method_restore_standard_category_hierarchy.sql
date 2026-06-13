UPDATE `payment_method_category`
SET `status` = 1,
    `updated_at` = NOW(),
    `del_flag` = 0
WHERE `tenant_id` = 1
  AND `id` IN (360104, 360331, 360332, 360341);

UPDATE `payment_method_category`
SET `category_code` = 'WALLET_QUICK',
    `category_name` = '钱包快捷',
    `status` = 1,
    `updated_at` = NOW(),
    `del_flag` = 0
WHERE `tenant_id` = 1
  AND `id` = 360341
  AND `parent_id` = 360105
  AND `level` = 3;

UPDATE `payment_method_category`
SET `status` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `id` IN (360106, 360107, 360371, 360372);

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'BANK_CARD',
    `interaction_type` = 'DEBIT_QUICK',
    `payment_material_type` = 'HTML_FORM',
    `requires_bank_selection` = 1,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `method_code` = 'PERSONAL_DEBIT_QUICK';

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'BANK_CARD',
    `interaction_type` = 'CREDIT_QUICK',
    `payment_material_type` = 'HTML_FORM',
    `requires_bank_selection` = 1,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `method_code` = 'PERSONAL_CREDIT_QUICK';

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'WALLET',
    `interaction_type` = 'WALLET_QUICK',
    `payment_material_type` = 'H5_PARAM',
    `requires_bank_selection` = 0,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `method_code` = 'PERSONAL_WALLET_QUICK';
