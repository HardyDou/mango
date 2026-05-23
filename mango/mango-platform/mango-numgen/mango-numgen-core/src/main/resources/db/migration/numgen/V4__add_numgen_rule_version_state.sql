SET @schema_name = DATABASE();

SET @add_rule_version_state = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_rule` ADD COLUMN `version_state` varchar(16) NOT NULL DEFAULT ''DRAFT'' COMMENT ''版本状态：DRAFT-草稿，ACTIVE-生效中，HISTORY-历史'' AFTER `publish_status`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'version_state'
);
PREPARE stmt FROM @add_rule_version_state;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `numgen_rule` r
LEFT JOIN `numgen_generator` g
  ON g.`tenant_id` = r.`tenant_id`
 AND g.`gen_key` = r.`gen_key`
 AND g.`del_flag` = 0
SET r.`version_state` = CASE
  WHEN r.`publish_status` = 1 THEN 'ACTIVE'
  WHEN g.`current_rule_version` IS NOT NULL AND r.`version` > g.`current_rule_version` THEN 'DRAFT'
  ELSE 'HISTORY'
END
WHERE r.`del_flag` = 0;

SET @add_rule_version_state_idx = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_rule` ADD KEY `idx_numgen_rule_tenant_key_state` (`tenant_id`, `gen_key`, `version_state`)', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND INDEX_NAME = 'idx_numgen_rule_tenant_key_state'
);
PREPARE stmt FROM @add_rule_version_state_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
