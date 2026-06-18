SET @menu_button_type_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'authorization_menu'
    AND COLUMN_NAME = 'button_type'
);
SET @add_menu_button_type_column_sql := IF(
  @menu_button_type_column_exists = 0,
  'ALTER TABLE `authorization_menu` ADD COLUMN `button_type` varchar(32) DEFAULT NULL COMMENT ''按钮类型：TABLE-表格按钮，NON_TABLE-非表格按钮'' AFTER `permissions`',
  'SELECT 1'
);
PREPARE add_menu_button_type_column_stmt FROM @add_menu_button_type_column_sql;
EXECUTE add_menu_button_type_column_stmt;
DEALLOCATE PREPARE add_menu_button_type_column_stmt;

SET @menu_button_display_rule_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'authorization_menu'
    AND COLUMN_NAME = 'button_display_rule'
);
SET @add_menu_button_display_rule_column_sql := IF(
  @menu_button_display_rule_column_exists = 0,
  'ALTER TABLE `authorization_menu` ADD COLUMN `button_display_rule` varchar(1000) DEFAULT NULL COMMENT ''按钮展示规则表达式'' AFTER `button_type`',
  'SELECT 1'
);
PREPARE add_menu_button_display_rule_column_stmt FROM @add_menu_button_display_rule_column_sql;
EXECUTE add_menu_button_display_rule_column_stmt;
DEALLOCATE PREPARE add_menu_button_display_rule_column_stmt;
