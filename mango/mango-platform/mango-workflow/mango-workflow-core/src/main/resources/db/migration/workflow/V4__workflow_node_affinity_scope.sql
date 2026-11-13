-- 节点亲和跨实例复用办理人：为业务申请登记稳定聚合键，并支撑按聚合键回溯历史办理人。
-- 条件执行：对表缺失的部分历史库、已手工加过列或索引的环境保持幂等，不中断升级。
-- Maven 1.0.20 发布版 V1 已包含 workflow_business_apply，正常升级路径会新增列和索引。

SET @workflow_affinity_key_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply'
      AND column_name = 'affinity_key'
  ),
  'DO 0',
  IF(
    EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'workflow_business_apply'
    ),
    'ALTER TABLE `workflow_business_apply` ADD COLUMN `affinity_key` varchar(128) DEFAULT NULL COMMENT ''稳定业务聚合键，跨提交保持不变'' AFTER `business_key`',
    'DO 0'
  )
);
PREPARE workflow_affinity_key_stmt FROM @workflow_affinity_key_ddl;
EXECUTE workflow_affinity_key_stmt;
DEALLOCATE PREPARE workflow_affinity_key_stmt;

SET @workflow_affinity_key_index_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply'
      AND index_name = 'idx_workflow_business_apply_affinity'
  ),
  'DO 0',
  IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'workflow_business_apply'
        AND column_name = 'affinity_key'
    ),
    'ALTER TABLE `workflow_business_apply` ADD KEY `idx_workflow_business_apply_affinity` (`tenant_id`, `business_type`, `affinity_key`, `process_instance_id`)',
    'DO 0'
  )
);
PREPARE workflow_affinity_key_index_stmt FROM @workflow_affinity_key_index_ddl;
EXECUTE workflow_affinity_key_index_stmt;
DEALLOCATE PREPARE workflow_affinity_key_index_stmt;
