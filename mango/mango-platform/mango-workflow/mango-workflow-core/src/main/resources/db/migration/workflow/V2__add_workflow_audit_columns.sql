-- Add audit columns that were absent from the Workflow V1 published in Mango Maven 1.0.20.
-- Each statement is conditional because fresh databases created by 1.0.21 or 1.0.22
-- already received these columns from their published V1.

SET @workflow_audit_column_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_task_record'
      AND column_name = 'created_by'
  ),
  'DO 0',
  'ALTER TABLE `workflow_task_record` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人'' AFTER `variables_json`'
);
PREPARE workflow_audit_column_stmt FROM @workflow_audit_column_ddl;
EXECUTE workflow_audit_column_stmt;
DEALLOCATE PREPARE workflow_audit_column_stmt;

SET @workflow_audit_column_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_copied_task'
      AND column_name = 'created_by'
  ),
  'DO 0',
  'ALTER TABLE `workflow_copied_task` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人'' AFTER `read_time`'
);
PREPARE workflow_audit_column_stmt FROM @workflow_audit_column_ddl;
EXECUTE workflow_audit_column_stmt;
DEALLOCATE PREPARE workflow_audit_column_stmt;

SET @workflow_audit_column_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_current_task'
      AND column_name = 'created_by'
  ),
  'DO 0',
  'ALTER TABLE `workflow_business_apply_current_task` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人'' AFTER `arrived_at`'
);
PREPARE workflow_audit_column_stmt FROM @workflow_audit_column_ddl;
EXECUTE workflow_audit_column_stmt;
DEALLOCATE PREPARE workflow_audit_column_stmt;

SET @workflow_audit_column_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_current_task'
      AND column_name = 'updated_by'
  ),
  'DO 0',
  'ALTER TABLE `workflow_business_apply_current_task` ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT ''更新人'' AFTER `created_at`'
);
PREPARE workflow_audit_column_stmt FROM @workflow_audit_column_ddl;
EXECUTE workflow_audit_column_stmt;
DEALLOCATE PREPARE workflow_audit_column_stmt;

SET @workflow_audit_column_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_status_log'
      AND column_name = 'created_by'
  ),
  'DO 0',
  'ALTER TABLE `workflow_business_apply_status_log` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人'' AFTER `process_instance_id`'
);
PREPARE workflow_audit_column_stmt FROM @workflow_audit_column_ddl;
EXECUTE workflow_audit_column_stmt;
DEALLOCATE PREPARE workflow_audit_column_stmt;

SET @workflow_audit_column_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_status_log'
      AND column_name = 'updated_by'
  ),
  'DO 0',
  'ALTER TABLE `workflow_business_apply_status_log` ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT ''更新人'' AFTER `created_at`'
);
PREPARE workflow_audit_column_stmt FROM @workflow_audit_column_ddl;
EXECUTE workflow_audit_column_stmt;
DEALLOCATE PREPARE workflow_audit_column_stmt;

SET @workflow_audit_column_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_status_log'
      AND column_name = 'updated_at'
  ),
  'DO 0',
  'ALTER TABLE `workflow_business_apply_status_log` ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `updated_by`'
);
PREPARE workflow_audit_column_stmt FROM @workflow_audit_column_ddl;
EXECUTE workflow_audit_column_stmt;
DEALLOCATE PREPARE workflow_audit_column_stmt;
