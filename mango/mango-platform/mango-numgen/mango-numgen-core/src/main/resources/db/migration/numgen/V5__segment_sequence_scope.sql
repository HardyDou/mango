SET @schema_name = DATABASE();

SET @add_segment_sequence_scope = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_rule_segment` ADD COLUMN `sequence_scope` tinyint NOT NULL DEFAULT ''0'' COMMENT ''是否参与流水分组：0-否，1-是'' AFTER `pad_char`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule_segment' AND COLUMN_NAME = 'sequence_scope'
);
PREPARE stmt FROM @add_segment_sequence_scope;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_sequence_scope_key = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_sequence` ADD COLUMN `scope_key` varchar(256) NOT NULL DEFAULT ''GLOBAL'' COMMENT ''流水分组键'' AFTER `rule_version`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND COLUMN_NAME = 'scope_key'
);
PREPARE stmt FROM @add_sequence_scope_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `numgen_sequence`
SET `scope_key` = 'GLOBAL'
WHERE `scope_key` IS NULL OR `scope_key` = '';

DELETE s1
FROM `numgen_sequence` s1
JOIN `numgen_sequence` s2
  ON s1.`tenant_id` = s2.`tenant_id`
 AND s1.`gen_key` = s2.`gen_key`
 AND s1.`scope_key` = s2.`scope_key`
 AND (
      s1.`current_value` < s2.`current_value`
      OR (s1.`current_value` = s2.`current_value` AND s1.`id` < s2.`id`)
 )
WHERE s1.`scope_key` = 'GLOBAL';

SET @drop_sequence_rule_key = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_sequence` DROP INDEX `uk_numgen_sequence_tenant_rule`', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND INDEX_NAME = 'uk_numgen_sequence_tenant_rule'
);
PREPARE stmt FROM @drop_sequence_rule_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_sequence_scope_unique_key = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_sequence` ADD UNIQUE KEY `uk_numgen_sequence_tenant_scope` (`tenant_id`, `gen_key`, `scope_key`)', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND INDEX_NAME = 'uk_numgen_sequence_tenant_scope'
);
PREPARE stmt FROM @add_sequence_scope_unique_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
