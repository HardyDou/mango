DELETE FROM `authorization_role_menu`
WHERE `menu_id` = 24007;

DELETE FROM `authorization_menu`
WHERE `id` = 24007
   OR `menu_code` = 'system:workflow:node-definition';
