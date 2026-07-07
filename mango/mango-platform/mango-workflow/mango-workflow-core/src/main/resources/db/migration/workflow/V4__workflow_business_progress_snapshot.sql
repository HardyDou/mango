SET @schema_name = DATABASE();

SET @add_workflow_current_task_claim_status = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `workflow_business_apply_current_task` ADD COLUMN `claim_status` varchar(32) NOT NULL DEFAULT ''ASSIGNED'' COMMENT ''认领状态: ASSIGNED-已分配 UNCLAIMED-待领取'' AFTER `assignee_name`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'workflow_business_apply_current_task'
    AND COLUMN_NAME = 'claim_status'
);
PREPARE stmt FROM @add_workflow_current_task_claim_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_workflow_current_task_candidate_users = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `workflow_business_apply_current_task` ADD COLUMN `candidate_users` varchar(1000) DEFAULT NULL COMMENT ''候选用户，逗号分隔'' AFTER `claim_status`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'workflow_business_apply_current_task'
    AND COLUMN_NAME = 'candidate_users'
);
PREPARE stmt FROM @add_workflow_current_task_candidate_users;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_workflow_current_task_candidate_groups = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `workflow_business_apply_current_task` ADD COLUMN `candidate_groups` varchar(1000) DEFAULT NULL COMMENT ''候选组，逗号分隔'' AFTER `candidate_users`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'workflow_business_apply_current_task'
    AND COLUMN_NAME = 'candidate_groups'
);
PREPARE stmt FROM @add_workflow_current_task_candidate_groups;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
