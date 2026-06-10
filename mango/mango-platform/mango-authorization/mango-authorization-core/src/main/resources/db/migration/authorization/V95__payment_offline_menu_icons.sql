UPDATE `authorization_menu`
SET `icon` = CASE `menu_code`
    WHEN 'payment:offline-payment' THEN 'CreditCard'
    WHEN 'payment:offline-collection' THEN 'Money'
    WHEN 'payment:offline-collection:refund' THEN 'RefreshLeft'
    WHEN 'payment:offline-refund' THEN 'RefreshLeft'
    ELSE `icon`
  END,
  `update_time` = NOW(),
  `updated_at` = NOW()
WHERE `app_code` = 'internal-admin'
  AND `module_code` = 'mango-payment'
  AND `menu_code` IN (
    'payment:offline-payment',
    'payment:offline-collection',
    'payment:offline-collection:refund',
    'payment:offline-refund'
  );
