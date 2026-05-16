CREATE TABLE IF NOT EXISTS `workflow_form_instance` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `process_instance_id` varchar(128) NOT NULL COMMENT '流程实例ID',
  `business_key` varchar(128) DEFAULT NULL COMMENT '业务主键',
  `definition_id` bigint DEFAULT NULL COMMENT 'Mango流程定义ID',
  `definition_key` varchar(128) DEFAULT NULL COMMENT '流程定义编码',
  `definition_name` varchar(128) DEFAULT NULL COMMENT '流程定义名称',
  `process_definition_id` varchar(128) DEFAULT NULL COMMENT 'Flowable流程定义ID',
  `process_definition_version` int DEFAULT NULL COMMENT 'Flowable流程版本',
  `form_code` varchar(128) DEFAULT NULL COMMENT '表单编码',
  `form_json` longtext COMMENT '表单JSON快照',
  `variables_json` longtext COMMENT '表单变量JSON快照',
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '状态: RUNNING/COMPLETED/REJECTED',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_form_instance_proc` (`process_instance_id`),
  KEY `idx_workflow_form_instance_definition` (`definition_id`),
  KEY `idx_workflow_form_instance_status` (`status`),
  KEY `idx_workflow_form_instance_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程实例表单快照';

CREATE TABLE IF NOT EXISTS `workflow_task_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `process_instance_id` varchar(128) NOT NULL COMMENT '流程实例ID',
  `task_id` varchar(128) DEFAULT NULL COMMENT '任务ID',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `task_definition_key` varchar(128) DEFAULT NULL COMMENT '任务定义Key',
  `action` varchar(32) NOT NULL COMMENT '动作: START/COMPLETE/REJECT',
  `action_name` varchar(64) NOT NULL COMMENT '动作名称',
  `operator_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '处理人',
  `comment` varchar(1000) DEFAULT NULL COMMENT '处理意见',
  `variables_json` longtext COMMENT '处理变量JSON',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_task_record_proc` (`process_instance_id`),
  KEY `idx_workflow_task_record_task` (`task_id`),
  KEY `idx_workflow_task_record_action` (`action`),
  KEY `idx_workflow_task_record_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流任务处理记录';

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2601001,1,'internal-admin',2601,3,'查询任务详情','workflow:task:detail',NULL,NULL,NULL,101,1,0,0,0,NULL,'workflow:task:detail',NULL,NULL,NOW(),NOW(),'查询工作流任务详情、表单与审批记录',0,NULL,NOW(),NULL,NOW()),
(2601002,1,'internal-admin',2601,3,'审批通过','workflow:task:complete',NULL,NULL,NULL,102,1,0,0,0,NULL,'workflow:task:complete',NULL,NULL,NOW(),NOW(),'完成工作流待办任务',0,NULL,NOW(),NULL,NOW()),
(2601003,1,'internal-admin',2601,3,'审批驳回','workflow:task:reject',NULL,NULL,NULL,103,1,0,0,0,NULL,'workflow:task:reject',NULL,NULL,NOW(),NOW(),'驳回并终止当前工作流实例',0,NULL,NOW(),NULL,NOW()),
(2602001,1,'internal-admin',2602,3,'查询流程详情','workflow:process:detail',NULL,NULL,NULL,104,1,0,0,0,NULL,'workflow:process:detail',NULL,NULL,NOW(),NOW(),'查询工作流实例详情与审批轨迹',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE `permissions` = VALUES(`permissions`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `del_flag` = VALUES(`del_flag`);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1102,1,1,2601001,102),
(1103,1,1,2601002,103),
(1104,1,1,2601003,104),
(1105,1,1,2602001,105),
(2055,1,2,2601001,55),
(2056,1,2,2601002,56),
(2057,1,2,2601003,57),
(2058,1,2,2602001,58);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(52601001,1,1,2601001,NOW(),NULL,NOW(),NULL,NOW()),
(52601002,1,1,2601002,NOW(),NULL,NOW(),NULL,NOW()),
(52601003,1,1,2601003,NOW(),NULL,NOW(),NULL,NOW()),
(52602001,1,1,2602001,NOW(),NULL,NOW(),NULL,NOW()),
(62601001,2,2,2601001,NOW(),NULL,NOW(),NULL,NOW()),
(62601002,2,2,2601002,NOW(),NULL,NOW(),NULL,NOW()),
(62601003,2,2,2601003,NOW(),NULL,NOW(),NULL,NOW()),
(62602001,2,2,2602001,NOW(),NULL,NOW(),NULL,NOW()),
(72601001,3,3,2601001,NOW(),NULL,NOW(),NULL,NOW()),
(72601002,3,3,2601002,NOW(),NULL,NOW(),NULL,NOW()),
(72601003,3,3,2601003,NOW(),NULL,NOW(),NULL,NOW()),
(72602001,3,3,2602001,NOW(),NULL,NOW(),NULL,NOW()),
(82601001,4,4,2601001,NOW(),NULL,NOW(),NULL,NOW()),
(82601002,4,4,2601002,NOW(),NULL,NOW(),NULL,NOW()),
(82601003,4,4,2601003,NOW(),NULL,NOW(),NULL,NOW()),
(82602001,4,4,2602001,NOW(),NULL,NOW(),NULL,NOW());
