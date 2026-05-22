SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'calendar_day' AND COLUMN_NAME = 'lunar_year') = 0,
  'ALTER TABLE `calendar_day` ADD COLUMN `lunar_year` int DEFAULT NULL COMMENT ''农历年'' AFTER `day_name`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'calendar_day' AND COLUMN_NAME = 'lunar_month') = 0,
  'ALTER TABLE `calendar_day` ADD COLUMN `lunar_month` tinyint DEFAULT NULL COMMENT ''农历月'' AFTER `lunar_year`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'calendar_day' AND COLUMN_NAME = 'lunar_day') = 0,
  'ALTER TABLE `calendar_day` ADD COLUMN `lunar_day` tinyint DEFAULT NULL COMMENT ''农历日'' AFTER `lunar_month`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'calendar_day' AND COLUMN_NAME = 'lunar_leap_month') = 0,
  'ALTER TABLE `calendar_day` ADD COLUMN `lunar_leap_month` tinyint NOT NULL DEFAULT ''0'' COMMENT ''是否农历闰月：1-是，0-否'' AFTER `lunar_day`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'calendar_day' AND COLUMN_NAME = 'lunar_text') = 0,
  'ALTER TABLE `calendar_day` ADD COLUMN `lunar_text` varchar(32) DEFAULT NULL COMMENT ''农历中文日期'' AFTER `lunar_leap_month`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'calendar_day' AND COLUMN_NAME = 'ganzhi_year') = 0,
  'ALTER TABLE `calendar_day` ADD COLUMN `ganzhi_year` varchar(16) DEFAULT NULL COMMENT ''干支纪年'' AFTER `lunar_text`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'calendar_day' AND COLUMN_NAME = 'zodiac') = 0,
  'ALTER TABLE `calendar_day` ADD COLUMN `zodiac` varchar(8) DEFAULT NULL COMMENT ''生肖'' AFTER `ganzhi_year`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'calendar_day' AND COLUMN_NAME = 'solar_term') = 0,
  'ALTER TABLE `calendar_day` ADD COLUMN `solar_term` varchar(16) DEFAULT NULL COMMENT ''节气'' AFTER `zodiac`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
