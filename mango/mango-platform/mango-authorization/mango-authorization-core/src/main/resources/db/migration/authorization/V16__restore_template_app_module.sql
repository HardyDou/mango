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
  (@template_module_id, 'internal-admin', 'mango-template', '模板管理模块', 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

UPDATE `authorization_menu`
SET `module_code` = 'mango-template',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `app_code` = 'internal-admin'
  AND (
    `menu_code` LIKE 'template%'
    OR `path` LIKE '/template%'
    OR `component` LIKE '%/template/%'
    OR `permissions` LIKE 'template:%'
  );

INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT COALESCE((SELECT MAX(`id`) FROM `authorization_role_menu`), 9000) + ROW_NUMBER() OVER (ORDER BY `role`.`id`, `menu`.`id`),
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
