SET @schema_name = DATABASE();

SET @drop_generator_description = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_generator` DROP COLUMN `description`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_generator' AND COLUMN_NAME = 'description'
);
PREPARE stmt FROM @drop_generator_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_generator_audit = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_generator` DROP COLUMN `current_audit_status`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_generator' AND COLUMN_NAME = 'current_audit_status'
);
PREPARE stmt FROM @drop_generator_audit;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_audit = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `audit_status`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'audit_status'
);
PREPARE stmt FROM @drop_rule_audit;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_scope_mode = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `scope_mode`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'scope_mode'
);
PREPARE stmt FROM @drop_rule_scope_mode;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_scope_param = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `scope_param`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'scope_param'
);
PREPARE stmt FROM @drop_rule_scope_param;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_reset_period = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `reset_period`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'reset_period'
);
PREPARE stmt FROM @drop_rule_reset_period;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_start_value = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `start_value`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'start_value'
);
PREPARE stmt FROM @drop_rule_start_value;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_step = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `step`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'step'
);
PREPARE stmt FROM @drop_rule_step;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_description = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `description`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'description'
);
PREPARE stmt FROM @drop_rule_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_segment_required = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule_segment` DROP COLUMN `required`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule_segment' AND COLUMN_NAME = 'required'
);
PREPARE stmt FROM @drop_segment_required;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_segment_default = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule_segment` DROP COLUMN `default_value`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule_segment' AND COLUMN_NAME = 'default_value'
);
PREPARE stmt FROM @drop_segment_default;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_segment_description = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule_segment` DROP COLUMN `description`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule_segment' AND COLUMN_NAME = 'description'
);
PREPARE stmt FROM @drop_segment_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_sequence_old_key = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_sequence` DROP INDEX `uk_numgen_sequence_tenant_scope`', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND INDEX_NAME = 'uk_numgen_sequence_tenant_scope'
);
PREPARE stmt FROM @drop_sequence_old_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_sequence_scope = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_sequence` DROP COLUMN `scope_key`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND COLUMN_NAME = 'scope_key'
);
PREPARE stmt FROM @drop_sequence_scope;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_sequence_period = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_sequence` DROP COLUMN `period_key`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND COLUMN_NAME = 'period_key'
);
PREPARE stmt FROM @drop_sequence_period;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_sequence_new_key = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_sequence` ADD UNIQUE KEY `uk_numgen_sequence_tenant_rule` (`tenant_id`, `gen_key`, `rule_version`)', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND INDEX_NAME = 'uk_numgen_sequence_tenant_rule'
);
PREPARE stmt FROM @add_sequence_new_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_history_scope = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_history` DROP COLUMN `scope_key`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_history' AND COLUMN_NAME = 'scope_key'
);
PREPARE stmt FROM @drop_history_scope;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_history_period = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_history` DROP COLUMN `period_key`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_history' AND COLUMN_NAME = 'period_key'
);
PREPARE stmt FROM @drop_history_period;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
