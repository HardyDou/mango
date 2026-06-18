SET @schema_name = DATABASE();

SET @add_biz_domain_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `biz_domain` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_domain' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_biz_domain_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
