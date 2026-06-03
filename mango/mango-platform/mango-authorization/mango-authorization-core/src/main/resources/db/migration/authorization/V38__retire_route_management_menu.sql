DELETE FROM `authorization_role_menu`
WHERE `menu_id` IN (21, 21001, 21002, 21003, 21004);

DELETE FROM `authorization_menu_package_item`
WHERE `menu_id` IN (21, 21001, 21002, 21003, 21004);

DELETE FROM `authorization_menu`
WHERE `id` IN (21001, 21002, 21003, 21004, 21)
   OR `menu_code` IN ('system:route', 'system:route:query', 'system:route:add', 'system:route:edit', 'system:route:delete')
   OR `permissions` IN ('system:route:list', 'system:route:query', 'system:route:add', 'system:route:edit', 'system:route:delete')
   OR `path` = '/system/route';
