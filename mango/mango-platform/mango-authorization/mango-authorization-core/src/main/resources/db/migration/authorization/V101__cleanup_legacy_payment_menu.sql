DELETE FROM `authorization_menu`
WHERE `module_code` = 'mango-payment'
  AND (
    `menu_code` IN (
      'payment:management',
      'payment:management:list',
      'payment:management:write',
      'payment:cashier',
      'payment:cashier:use'
    )
    OR `parent_id` IN (2720, 2721, 2722)
    OR (`menu_code` = 'payment' AND `id` <> 2800)
  );
