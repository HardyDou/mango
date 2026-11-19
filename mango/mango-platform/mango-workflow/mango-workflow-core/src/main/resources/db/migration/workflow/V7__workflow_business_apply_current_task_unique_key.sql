-- 业务「当前任务」视图补 (tenant_id, task_id) 唯一键（Mango Issue #1289）。
-- 一 task 一行目前只靠 WorkflowBusinessApplyService#refreshCurrentTasks「按申请删除后重建」保证；
-- 并发触发两次刷新或人工补数会留下重复行，任务分页左连接该表后会把同一任务翻倍成多行。
--
-- 保留策略（可审计）：同 (tenant_id, task_id) 只保留 updated_at 最新的一行；
-- updated_at 相同时保留 id 最大的一行，即最后写入的那一行。
-- task_id 为空的行表示任务尚未产生或已清空，相互之间不构成重复，不参与清理也不受唯一键约束
-- （MySQL 唯一键允许多个 NULL）。
--
-- 两条语句都做成条件执行：唯一键已存在时跳过，重复执行不会失败，也不会重复删除。

SET @workflow_current_task_dedupe = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_current_task'
      AND column_name = 'task_id'
  ),
  'DELETE duplicate_row FROM `workflow_business_apply_current_task` duplicate_row
     JOIN `workflow_business_apply_current_task` kept_row
       ON kept_row.`tenant_id` = duplicate_row.`tenant_id`
      AND kept_row.`task_id` = duplicate_row.`task_id`
      AND (kept_row.`updated_at` > duplicate_row.`updated_at`
           OR (kept_row.`updated_at` = duplicate_row.`updated_at`
               AND kept_row.`id` > duplicate_row.`id`))
    WHERE duplicate_row.`task_id` IS NOT NULL',
  'DO 0'
);
PREPARE workflow_current_task_dedupe_stmt FROM @workflow_current_task_dedupe;
EXECUTE workflow_current_task_dedupe_stmt;
DEALLOCATE PREPARE workflow_current_task_dedupe_stmt;

-- 老版本的当前任务表没有 task_id（1.0.20 夹具即如此），此时整条语句跳过，不中断升级。
SET @workflow_current_task_unique_key = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_current_task'
      AND column_name = 'task_id'
  )
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_current_task'
      AND index_name = 'uk_workflow_business_apply_current_task'
  ),
  'ALTER TABLE `workflow_business_apply_current_task` ADD UNIQUE KEY `uk_workflow_business_apply_current_task` (`tenant_id`, `task_id`)',
  'DO 0'
);
PREPARE workflow_current_task_unique_key_stmt FROM @workflow_current_task_unique_key;
EXECUTE workflow_current_task_unique_key_stmt;
DEALLOCATE PREPARE workflow_current_task_unique_key_stmt;
