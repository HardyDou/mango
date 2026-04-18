SET @old_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_kv_record'
);

SET @new_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'infra_kv_entry'
);

SET @rename_sql := IF(
    @old_exists = 1 AND @new_exists = 0,
    'RENAME TABLE `sys_kv_record` TO `infra_kv_entry`',
    'DO 0'
);

PREPARE rename_stmt FROM @rename_sql;
EXECUTE rename_stmt;
DEALLOCATE PREPARE rename_stmt;
