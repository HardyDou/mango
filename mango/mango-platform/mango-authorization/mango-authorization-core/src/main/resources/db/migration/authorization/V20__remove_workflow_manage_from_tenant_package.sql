DELETE `role_menu`
FROM `authorization_role_menu` `role_menu`
JOIN `authorization_role` `role`
  ON `role`.`id` = `role_menu`.`role_id`
JOIN `authorization_menu` `menu`
  ON `menu`.`id` = `role_menu`.`menu_id`
LEFT JOIN `authorization_menu` `parent`
  ON `parent`.`id` = `menu`.`parent_id`
LEFT JOIN `authorization_menu` `grand_parent`
  ON `grand_parent`.`id` = `parent`.`parent_id`
WHERE `role`.`id` IN (2, 3, 4)
  AND (
    `menu`.`id` = 2604
    OR `parent`.`id` = 2604
    OR `grand_parent`.`id` = 2604
  );

DELETE `package_item`
FROM `authorization_menu_package_item` `package_item`
JOIN `authorization_menu` `menu`
  ON `menu`.`id` = `package_item`.`menu_id`
LEFT JOIN `authorization_menu` `parent`
  ON `parent`.`id` = `menu`.`parent_id`
LEFT JOIN `authorization_menu` `grand_parent`
  ON `grand_parent`.`id` = `parent`.`parent_id`
WHERE `package_item`.`package_id` = 2
  AND (
    `menu`.`id` = 2604
    OR `parent`.`id` = 2604
    OR `grand_parent`.`id` = 2604
  );
