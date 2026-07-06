ALTER TABLE `workflow_business_apply_current_task`
  ADD COLUMN `claim_status` varchar(32) NOT NULL DEFAULT 'ASSIGNED' COMMENT '认领状态: ASSIGNED-已分配 UNCLAIMED-待领取' AFTER `assignee_name`;

ALTER TABLE `workflow_business_apply_current_task`
  ADD COLUMN `candidate_users` varchar(1000) DEFAULT NULL COMMENT '候选用户，逗号分隔' AFTER `claim_status`;

ALTER TABLE `workflow_business_apply_current_task`
  ADD COLUMN `candidate_groups` varchar(1000) DEFAULT NULL COMMENT '候选组，逗号分隔' AFTER `candidate_users`;
