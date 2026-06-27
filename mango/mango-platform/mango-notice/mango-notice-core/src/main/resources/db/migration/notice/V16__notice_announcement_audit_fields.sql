SET @schema_name = DATABASE();

SET @add_notice_announcement_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `notice_announcement` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''组织 ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'notice_announcement' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_notice_announcement_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_notice_announcement_target_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `notice_announcement_target` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''组织 ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'notice_announcement_target' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_notice_announcement_target_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_notice_announcement_target_created_by = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `notice_announcement_target` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人 ID'' AFTER `org_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'notice_announcement_target' AND COLUMN_NAME = 'created_by'
);
PREPARE stmt FROM @add_notice_announcement_target_created_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_notice_announcement_target_updated_by = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `notice_announcement_target` ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT ''更新人 ID'' AFTER `created_at`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'notice_announcement_target' AND COLUMN_NAME = 'updated_by'
);
PREPARE stmt FROM @add_notice_announcement_target_updated_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_notice_announcement_target_updated_at = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `notice_announcement_target` ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `updated_by`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'notice_announcement_target' AND COLUMN_NAME = 'updated_at'
);
PREPARE stmt FROM @add_notice_announcement_target_updated_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_notice_announcement_recipient_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `notice_announcement_recipient` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''组织 ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'notice_announcement_recipient' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_notice_announcement_recipient_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_notice_announcement_recipient_created_by = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `notice_announcement_recipient` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人 ID'' AFTER `org_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'notice_announcement_recipient' AND COLUMN_NAME = 'created_by'
);
PREPARE stmt FROM @add_notice_announcement_recipient_created_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_notice_announcement_recipient_updated_by = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `notice_announcement_recipient` ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT ''更新人 ID'' AFTER `created_at`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'notice_announcement_recipient' AND COLUMN_NAME = 'updated_by'
);
PREPARE stmt FROM @add_notice_announcement_recipient_updated_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
