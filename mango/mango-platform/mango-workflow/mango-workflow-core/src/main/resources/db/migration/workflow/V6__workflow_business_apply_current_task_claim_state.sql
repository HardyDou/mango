-- 业务「当前任务」视图补齐释放资格所需字段（Mango Issue #1282）。
-- assign_source 记录当前 assignee 的来源，claim_time 记录办理人获得任务的时间，
-- 业务列表据此逐行判定是否展示释放按钮，不再为每行调用任务详情接口。
-- 两条语句都做成条件 DDL：新建库的当前任务表由 V1 建立，升级库由本脚本补列，重复执行不会失败。
SET @workflow_current_task_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_current_task'
      AND column_name = 'assign_source'
  ),
  'DO 0',
  'ALTER TABLE `workflow_business_apply_current_task` ADD COLUMN `assign_source` varchar(64) DEFAULT NULL COMMENT ''承办人来源'' AFTER `assignee_name`'
);
PREPARE workflow_current_task_stmt FROM @workflow_current_task_ddl;
EXECUTE workflow_current_task_stmt;
DEALLOCATE PREPARE workflow_current_task_stmt;

SET @workflow_current_task_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'workflow_business_apply_current_task'
      AND column_name = 'claim_time'
  ),
  'DO 0',
  'ALTER TABLE `workflow_business_apply_current_task` ADD COLUMN `claim_time` datetime DEFAULT NULL COMMENT ''办理人获得任务的时间'' AFTER `arrived_at`'
);
PREPARE workflow_current_task_stmt FROM @workflow_current_task_ddl;
EXECUTE workflow_current_task_stmt;
DEALLOCATE PREPARE workflow_current_task_stmt;
