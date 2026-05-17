SET @rename_file_record = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_file_record')
        AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'file_record'),
        'RENAME TABLE `sys_file_record` TO `file_record`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_file_record;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_file_storage_config = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_file_storage_config')
        AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'file_storage_config'),
        'RENAME TABLE `sys_file_storage_config` TO `file_storage_config`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_file_storage_config;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_file_settings = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_file_settings')
        AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'file_settings'),
        'RENAME TABLE `sys_file_settings` TO `file_settings`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_file_settings;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_file_directory = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_file_directory')
        AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'file_directory'),
        'RENAME TABLE `sys_file_directory` TO `file_directory`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_file_directory;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
