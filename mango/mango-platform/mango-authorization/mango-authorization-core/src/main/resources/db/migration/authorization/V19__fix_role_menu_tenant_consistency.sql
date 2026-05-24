UPDATE `authorization_role_menu` `role_menu`
JOIN `authorization_role` `role`
  ON `role`.`id` = `role_menu`.`role_id`
SET `role_menu`.`tenant_id` = `role`.`tenant_id`,
    `role_menu`.`updated_at` = CURRENT_TIMESTAMP
WHERE `role_menu`.`tenant_id` <> `role`.`tenant_id`;
