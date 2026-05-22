SET @template_module_id := (
  SELECT `id`
  FROM `authorization_app_module`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-template'
  LIMIT 1
);

SET @template_module_id := COALESCE(@template_module_id, (
  SELECT COALESCE(MAX(`id`), 0) + 1
  FROM `authorization_app_module`
));

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (@template_module_id, 'internal-admin', 'mango-template', '模板中心模块', 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

SET @template_menu_item_base := (
  SELECT COALESCE(MAX(`id`), 1000)
  FROM `authorization_menu_package_item`
);

INSERT INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`)
SELECT @template_menu_item_base := @template_menu_item_base + 1,
       `menu`.`tenant_id`,
       1,
       `menu`.`id`,
       120 + `seq`.`sort_no`
FROM (
  SELECT 29 AS `menu_id`, 0 AS `sort_no` UNION ALL
  SELECT 2902, 1 UNION ALL
  SELECT 2901, 2 UNION ALL
  SELECT 2903, 3 UNION ALL
  SELECT 290200, 4 UNION ALL
  SELECT 290201, 5 UNION ALL
  SELECT 290202, 6 UNION ALL
  SELECT 290203, 7 UNION ALL
  SELECT 290204, 8 UNION ALL
  SELECT 290205, 9 UNION ALL
  SELECT 290100, 10 UNION ALL
  SELECT 290101, 11 UNION ALL
  SELECT 290102, 12 UNION ALL
  SELECT 290103, 13 UNION ALL
  SELECT 290104, 14 UNION ALL
  SELECT 290105, 15 UNION ALL
  SELECT 290106, 16 UNION ALL
  SELECT 290107, 17 UNION ALL
  SELECT 290108, 18 UNION ALL
  SELECT 290300, 19 UNION ALL
  SELECT 290301, 20
) `seq`
JOIN `authorization_menu` `menu` ON `menu`.`id` = `seq`.`menu_id`
WHERE NOT EXISTS (
  SELECT 1
  FROM `authorization_menu_package_item` `item`
  WHERE `item`.`tenant_id` = `menu`.`tenant_id`
    AND `item`.`package_id` = 1
    AND `item`.`menu_id` = `menu`.`id`
);

SET @template_role_menu_base := (
  SELECT COALESCE(MAX(`id`), 9000)
  FROM `authorization_role_menu`
);

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT @template_role_menu_base := @template_role_menu_base + 1,
       `role`.`tenant_id`,
       `role`.`id`,
       `menu`.`id`,
       NOW(),
       NULL,
       NOW(),
       NULL,
       NOW()
FROM `authorization_role` `role`
JOIN `authorization_menu` `menu`
  ON `menu`.`app_code` = `role`.`app_code`
 AND `menu`.`menu_code` LIKE 'template%'
WHERE `role`.`app_code` = 'internal-admin'
  AND `role`.`role_code` = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM `authorization_role_menu` `role_menu`
    WHERE `role_menu`.`role_id` = `role`.`id`
      AND `role_menu`.`menu_id` = `menu`.`id`
  );
