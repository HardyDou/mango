ALTER TABLE `payment_refund_approval`
  ADD COLUMN `workflow_apply_id` bigint DEFAULT NULL COMMENT '工作流申请ID' AFTER `status`,
  ADD COLUMN `workflow_process_instance_id` varchar(128) DEFAULT NULL COMMENT '工作流流程实例ID' AFTER `workflow_apply_id`,
  ADD COLUMN `workflow_process_definition_key` varchar(128) DEFAULT NULL COMMENT '工作流流程定义编码' AFTER `workflow_process_instance_id`,
  ADD COLUMN `workflow_apply_status` varchar(64) DEFAULT NULL COMMENT '工作流申请状态' AFTER `workflow_process_definition_key`,
  ADD COLUMN `workflow_apply_status_name` varchar(128) DEFAULT NULL COMMENT '工作流申请状态名称' AFTER `workflow_apply_status`,
  ADD COLUMN `workflow_current_task_names` varchar(512) DEFAULT NULL COMMENT '工作流当前节点名称' AFTER `workflow_apply_status_name`,
  ADD COLUMN `workflow_current_assignee_names` varchar(512) DEFAULT NULL COMMENT '工作流当前处理人名称' AFTER `workflow_current_task_names`,
  ADD COLUMN `workflow_synced_at` datetime DEFAULT NULL COMMENT '工作流状态同步时间' AFTER `workflow_current_assignee_names`;

ALTER TABLE `payment_refund_approval`
  ADD KEY `idx_payment_refund_approval_workflow_apply` (`tenant_id`, `workflow_apply_id`),
  ADD KEY `idx_payment_refund_approval_workflow_instance` (`tenant_id`, `workflow_process_instance_id`);
