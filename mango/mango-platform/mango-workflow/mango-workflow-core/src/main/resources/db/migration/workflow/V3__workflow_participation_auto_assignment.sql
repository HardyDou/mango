CREATE TABLE IF NOT EXISTS `workflow_process_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `process_key` varchar(128) NOT NULL COMMENT '流程编码',
  `business_key` varchar(128) NOT NULL COMMENT '业务主键',
  `process_instance_id` varchar(128) NOT NULL COMMENT '流程实例ID',
  `user_id` bigint NOT NULL COMMENT '稳定用户ID',
  `member_id` bigint DEFAULT NULL COMMENT '租户成员ID快照',
  `username_snapshot` varchar(128) DEFAULT NULL COMMENT '用户名快照',
  `display_name_snapshot` varchar(128) DEFAULT NULL COMMENT '显示名称快照',
  `participant_type` varchar(32) NOT NULL COMMENT '参与类型',
  `active` tinyint NOT NULL DEFAULT 1 COMMENT '是否为有效只读关系',
  `first_participated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次参与时间',
  `last_participated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近参与时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_participant_instance_user_type`
    (`tenant_id`, `process_instance_id`, `user_id`, `participant_type`),
  KEY `idx_workflow_participant_business_access`
    (`tenant_id`, `process_key`, `business_key`, `user_id`, `active`),
  KEY `idx_workflow_participant_user_page`
    (`tenant_id`, `user_id`, `active`, `last_participated_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流参与关系投影';

CREATE TABLE IF NOT EXISTS `workflow_auto_assignment_state` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `process_definition_id` varchar(128) NOT NULL COMMENT 'Flowable流程定义ID',
  `task_definition_key` varchar(128) NOT NULL COMMENT '任务定义Key',
  `last_assigned_user_id` bigint DEFAULT NULL COMMENT '上次派单用户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_auto_assignment_node`
    (`tenant_id`, `process_definition_id`, `task_definition_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流自动派单游标';

INSERT INTO `workflow_process_participant` (
  `tenant_id`, `process_key`, `business_key`, `process_instance_id`, `user_id`,
  `username_snapshot`, `display_name_snapshot`, `participant_type`, `active`,
  `first_participated_at`, `last_participated_at`, `created_by`, `created_at`, `updated_by`, `updated_at`
)
SELECT
  wtr.`tenant_id`, wfi.`definition_key`, wfi.`business_key`, wtr.`process_instance_id`, wtr.`operator_id`,
  MAX(wtr.`operator_name`), MAX(wtr.`operator_name`),
  CASE WHEN wtr.`action` = 'START' THEN 'INITIATOR' ELSE 'COMPLETED_HANDLER' END,
  1, MIN(wtr.`created_time`), MAX(wtr.`created_time`), MIN(wtr.`operator_id`),
  MIN(wtr.`created_at`), MAX(wtr.`operator_id`), MAX(wtr.`updated_at`)
FROM `workflow_task_record` wtr
JOIN `workflow_form_instance` wfi
  ON wfi.`tenant_id` = wtr.`tenant_id`
 AND wfi.`process_instance_id` = wtr.`process_instance_id`
WHERE wtr.`operator_id` IS NOT NULL
  AND wfi.`definition_key` IS NOT NULL
  AND wfi.`business_key` IS NOT NULL
  AND wtr.`action` IN ('START', 'COMPLETE', 'REJECT', 'RETURN')
GROUP BY wtr.`tenant_id`, wfi.`definition_key`, wfi.`business_key`,
         wtr.`process_instance_id`, wtr.`operator_id`,
         CASE WHEN wtr.`action` = 'START' THEN 'INITIATOR' ELSE 'COMPLETED_HANDLER' END
ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO `workflow_process_participant` (
  `tenant_id`, `process_key`, `business_key`, `process_instance_id`, `user_id`,
  `username_snapshot`, `display_name_snapshot`, `participant_type`, `active`,
  `first_participated_at`, `last_participated_at`, `created_by`, `created_at`, `updated_by`, `updated_at`
)
SELECT
  wct.`tenant_id`, wfi.`definition_key`, wct.`business_key`, wct.`process_instance_id`, wct.`assignee_id`,
  MAX(wct.`assignee_name`), MAX(wct.`assignee_name`), 'CURRENT_ASSIGNEE', 1,
  MIN(COALESCE(wct.`arrived_at`, wct.`created_at`)), MAX(wct.`updated_at`),
  MIN(wct.`created_by`), MIN(wct.`created_at`), MAX(wct.`updated_by`), MAX(wct.`updated_at`)
FROM `workflow_business_apply_current_task` wct
JOIN `workflow_form_instance` wfi
  ON wfi.`tenant_id` = wct.`tenant_id`
 AND wfi.`process_instance_id` = wct.`process_instance_id`
WHERE wct.`assignee_id` IS NOT NULL
  AND wfi.`definition_key` IS NOT NULL
  AND wct.`business_key` IS NOT NULL
GROUP BY wct.`tenant_id`, wfi.`definition_key`, wct.`business_key`,
         wct.`process_instance_id`, wct.`assignee_id`
ON DUPLICATE KEY UPDATE `id` = `id`;
