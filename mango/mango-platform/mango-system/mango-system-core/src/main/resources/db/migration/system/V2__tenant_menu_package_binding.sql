SET @package_id_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_tenant'
    AND column_name = 'package_id'
);

SET @add_package_id_sql := IF(
  @package_id_column_exists = 0,
  'ALTER TABLE `sys_tenant` ADD COLUMN `package_id` bigint DEFAULT NULL COMMENT ''菜单授权套餐ID'' AFTER `capability_codes`',
  'SELECT 1'
);

PREPARE add_package_id_stmt FROM @add_package_id_sql;
EXECUTE add_package_id_stmt;
DEALLOCATE PREPARE add_package_id_stmt;

UPDATE `sys_tenant`
SET `package_id` = CASE WHEN `id` = 1 THEN 1 ELSE 2 END
WHERE `package_id` IS NULL;
