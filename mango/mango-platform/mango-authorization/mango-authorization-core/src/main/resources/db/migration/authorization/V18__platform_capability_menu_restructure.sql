UPDATE `authorization_menu`
SET `menu_name` = '平台能力',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2700 OR `menu_code` = 'data';

SET @file_module_id := (
  SELECT `id`
  FROM `authorization_app_module`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-file'
  LIMIT 1
);

SET @file_module_id := COALESCE(@file_module_id, (
  SELECT COALESCE(MAX(`id`), 0) + 1
  FROM `authorization_app_module`
));

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (@file_module_id, 'internal-admin', 'mango-file', '文件管理模块', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

UPDATE `authorization_app_module`
SET `module_name` = '模板管理模块',
    `update_time` = CURRENT_TIMESTAMP
WHERE `app_code` = 'internal-admin'
  AND `module_code` = 'mango-template';

UPDATE `authorization_menu`
SET `parent_id` = 2700,
    `menu_name` = '文件管理',
    `module_code` = 'mango-file',
    `sort` = 3,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 28 OR `menu_code` = 'file';

UPDATE `authorization_menu`
SET `module_code` = 'mango-file',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `app_code` = 'internal-admin'
  AND (
    `menu_code` LIKE 'file%'
    OR `path` LIKE '/file%'
    OR `component` LIKE '%/file/%'
    OR `permissions` LIKE 'file:%'
  );

UPDATE `authorization_menu`
SET `parent_id` = 2700,
    `menu_name` = '模板管理',
    `sort` = 4,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 29 OR `menu_code` = 'template';
