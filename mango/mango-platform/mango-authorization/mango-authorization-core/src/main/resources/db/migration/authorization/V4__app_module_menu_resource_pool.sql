CREATE TABLE IF NOT EXISTS `authorization_app_module` (
  `id` bigint NOT NULL COMMENT '主键',
  `app_code` varchar(64) NOT NULL COMMENT '逻辑应用编码',
  `module_code` varchar(128) NOT NULL COMMENT '能力模块编码',
  `module_name` varchar(128) NOT NULL COMMENT '能力模块名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_app_module` (`app_code`,`module_code`),
  KEY `idx_authorization_app_module_app` (`app_code`),
  KEY `idx_authorization_app_module_module` (`module_code`),
  KEY `idx_authorization_app_module_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='逻辑应用集成能力模块表';

SET @menu_module_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'authorization_menu'
    AND COLUMN_NAME = 'module_code'
);
SET @add_menu_module_column_sql := IF(
  @menu_module_column_exists = 0,
  'ALTER TABLE `authorization_menu` ADD COLUMN `module_code` varchar(128) NOT NULL DEFAULT ''mango-system'' COMMENT ''能力模块编码'' AFTER `app_code`',
  'SELECT 1'
);
PREPARE add_menu_module_column_stmt FROM @add_menu_module_column_sql;
EXECUTE add_menu_module_column_stmt;
DEALLOCATE PREPARE add_menu_module_column_stmt;

SET @menu_module_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'authorization_menu'
    AND INDEX_NAME = 'idx_authorization_menu_module_code'
);
SET @add_menu_module_index_sql := IF(
  @menu_module_index_exists = 0,
  'ALTER TABLE `authorization_menu` ADD KEY `idx_authorization_menu_module_code` (`module_code`)',
  'SELECT 1'
);
PREPARE add_menu_module_index_stmt FROM @add_menu_module_index_sql;
EXECUTE add_menu_module_index_stmt;
DEALLOCATE PREPARE add_menu_module_index_stmt;

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (1, 'internal-admin', 'mango-authorization', 'mango-authorization', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'internal-admin', 'mango-system', 'mango-system', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'internal-admin', 'mango-workflow', 'mango-workflow', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

UPDATE `authorization_menu`
SET `module_code` = 'mango-workflow'
WHERE `app_code` = 'internal-admin'
  AND (
    `menu_code` LIKE 'workflow:%'
    OR `path` LIKE '/workflow%'
    OR `component` LIKE '%/workflow/%'
    OR `permissions` LIKE 'workflow:%'
  );

UPDATE `authorization_menu`
SET `module_code` = 'mango-authorization'
WHERE `app_code` = 'internal-admin'
  AND (
    `menu_code` LIKE 'authorization:%'
    OR `permissions` LIKE 'authorization:%'
    OR `menu_code` IN ('system:permission', 'system:account-access', 'system:role', 'system:menu', 'system:menu-package')
    OR `permissions` IN ('system:menu:list', 'system:menu:query', 'system:menu:add', 'system:menu:edit', 'system:menu:delete',
                         'system:menu-package:list', 'system:menu-package:query', 'system:menu-package:add',
                         'system:menu-package:edit', 'system:menu-package:delete')
  );

UPDATE `authorization_menu`
SET `module_code` = 'mango-system'
WHERE `app_code` = 'internal-admin'
  AND (`module_code` IS NULL OR `module_code` = '' OR `module_code` = 'mango-system');
