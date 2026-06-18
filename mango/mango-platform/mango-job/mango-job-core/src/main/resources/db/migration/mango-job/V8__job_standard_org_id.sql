SET @schema_name = DATABASE();

SET @add_job_definition_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_definition` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_definition' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_definition_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_instance_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_instance` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_instance' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_instance_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_log_index_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_log_index` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_log_index' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_log_index_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_worker_snapshot_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_worker_snapshot` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_worker_snapshot' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_worker_snapshot_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_alarm_rule_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_alarm_rule` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_alarm_rule' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_alarm_rule_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_engine_mapping_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_engine_mapping` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_engine_mapping' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_engine_mapping_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_operation_log_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_operation_log` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_operation_log' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_operation_log_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_schedule_cursor_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_schedule_cursor` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_schedule_cursor' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_schedule_cursor_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_attempt_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_attempt` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_attempt' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_attempt_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_worker_capability_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_worker_capability` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_worker_capability' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_worker_capability_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_log_chunk_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_log_chunk` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_log_chunk' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_log_chunk_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_job_event_org_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `mango_job_event` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''所属组织ID'' AFTER `tenant_id`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mango_job_event' AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_job_event_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
