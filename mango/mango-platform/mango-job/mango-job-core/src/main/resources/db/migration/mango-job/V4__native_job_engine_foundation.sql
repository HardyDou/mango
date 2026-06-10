-- Mango native Job Engine foundation.

ALTER TABLE `mango_job_definition`
  ADD COLUMN `module_code` varchar(128) DEFAULT NULL COMMENT '所属模块编码';

ALTER TABLE `mango_job_definition`
  ADD COLUMN `handler_version` varchar(64) DEFAULT NULL COMMENT '处理器版本';

ALTER TABLE `mango_job_definition`
  ADD COLUMN `timezone` varchar(64) DEFAULT NULL COMMENT '调度时区';

ALTER TABLE `mango_job_definition`
  ADD COLUMN `max_retry_count` int DEFAULT NULL COMMENT '最大重试次数';

ALTER TABLE `mango_job_definition`
  ADD COLUMN `version` int NOT NULL DEFAULT '0' COMMENT '定义版本';

ALTER TABLE `mango_job_definition`
  ADD COLUMN `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除';

ALTER TABLE `mango_job_definition`
  ADD KEY `idx_job_native_handler` (`tenant_id`, `app_code`, `handler_name`, `status`);

ALTER TABLE `mango_job_instance`
  ADD COLUMN `job_code` varchar(128) DEFAULT NULL COMMENT '任务编码快照';

ALTER TABLE `mango_job_instance`
  ADD COLUMN `job_name_snapshot` varchar(128) DEFAULT NULL COMMENT '任务名称快照';

ALTER TABLE `mango_job_instance`
  ADD COLUMN `idempotency_key` varchar(256) DEFAULT NULL COMMENT '幂等键';

ALTER TABLE `mango_job_instance`
  ADD COLUMN `scheduled_fire_time` datetime DEFAULT NULL COMMENT '计划触发时间';

ALTER TABLE `mango_job_instance`
  ADD COLUMN `actual_fire_time` datetime DEFAULT NULL COMMENT '实际触发时间';

ALTER TABLE `mango_job_instance`
  ADD COLUMN `attempt_count` int NOT NULL DEFAULT '0' COMMENT '执行尝试次数';

ALTER TABLE `mango_job_instance`
  ADD COLUMN `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间';

ALTER TABLE `mango_job_instance`
  ADD COLUMN `retry_reason` varchar(1024) DEFAULT NULL COMMENT '重试原因';

ALTER TABLE `mango_job_instance`
  ADD COLUMN `result_summary` varchar(2048) DEFAULT NULL COMMENT '结果摘要';

ALTER TABLE `mango_job_instance`
  ADD UNIQUE KEY `uk_instance_idempotency` (`tenant_id`, `job_id`, `idempotency_key`);

ALTER TABLE `mango_job_instance`
  ADD KEY `idx_instance_native_status` (`tenant_id`, `status`, `scheduled_fire_time`);

CREATE TABLE IF NOT EXISTS `mango_job_schedule_cursor` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `job_id` bigint NOT NULL COMMENT 'Mango 任务 ID',
  `schedule_version` int NOT NULL DEFAULT '0' COMMENT '调度版本',
  `last_fire_time` datetime DEFAULT NULL COMMENT '上次触发时间',
  `next_fire_time` datetime DEFAULT NULL COMMENT '下次触发时间',
  `misfire_policy` varchar(64) DEFAULT NULL COMMENT '错过触发策略',
  `lock_owner` varchar(128) DEFAULT NULL COMMENT '调度锁持有者',
  `lock_until` datetime DEFAULT NULL COMMENT '调度锁过期时间',
  `last_scan_at` datetime DEFAULT NULL COMMENT '最近扫描时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_cursor_job` (`job_id`),
  KEY `idx_schedule_due` (`tenant_id`, `next_fire_time`, `lock_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Mango Job 调度游标';

CREATE TABLE IF NOT EXISTS `mango_job_attempt` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `instance_id` bigint NOT NULL COMMENT 'Mango 实例 ID',
  `job_id` bigint NOT NULL COMMENT 'Mango 任务 ID',
  `attempt_no` int NOT NULL COMMENT '尝试序号',
  `worker_id` bigint DEFAULT NULL COMMENT 'Worker ID',
  `worker_address_snapshot` varchar(256) DEFAULT NULL COMMENT 'Worker 地址快照',
  `status` varchar(32) NOT NULL COMMENT '尝试状态',
  `lease_owner` varchar(128) DEFAULT NULL COMMENT '租约持有者',
  `lease_until` datetime DEFAULT NULL COMMENT '租约过期时间',
  `fencing_token` varchar(128) DEFAULT NULL COMMENT '防陈旧写 token',
  `dispatch_time` datetime DEFAULT NULL COMMENT '分发时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `last_heartbeat_at` datetime DEFAULT NULL COMMENT '最近执行心跳',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `exit_code` varchar(64) DEFAULT NULL COMMENT '退出码',
  `error_summary` varchar(1024) DEFAULT NULL COMMENT '错误摘要',
  `result_payload` text DEFAULT NULL COMMENT '执行结果 payload',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attempt_no` (`instance_id`, `attempt_no`),
  KEY `idx_attempt_status_lease` (`tenant_id`, `status`, `lease_until`),
  KEY `idx_attempt_worker` (`tenant_id`, `worker_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Mango Job 执行尝试';

CREATE TABLE IF NOT EXISTS `mango_job_worker_capability` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `worker_id` bigint NOT NULL COMMENT 'Worker ID',
  `app_code` varchar(128) NOT NULL COMMENT '所属逻辑应用',
  `handler_name` varchar(256) NOT NULL COMMENT '处理器名称',
  `handler_version` varchar(64) DEFAULT NULL COMMENT '处理器版本',
  `param_schema_hash` varchar(128) DEFAULT NULL COMMENT '参数 schema 摘要',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_worker_handler` (`worker_id`, `app_code`, `handler_name`, `handler_version`),
  KEY `idx_capability_handler` (`tenant_id`, `app_code`, `handler_name`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Mango Job Worker 能力';

CREATE TABLE IF NOT EXISTS `mango_job_log_chunk` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `instance_id` bigint NOT NULL COMMENT 'Mango 实例 ID',
  `attempt_id` bigint DEFAULT NULL COMMENT '执行尝试 ID',
  `sequence_no` bigint NOT NULL COMMENT '日志序号',
  `log_time` datetime NOT NULL COMMENT '日志时间',
  `level` varchar(32) DEFAULT NULL COMMENT '日志级别',
  `logger_name` varchar(256) DEFAULT NULL COMMENT 'Logger 名称',
  `thread_name` varchar(256) DEFAULT NULL COMMENT '线程名',
  `content` text NOT NULL COMMENT '日志内容',
  `content_hash` varchar(128) DEFAULT NULL COMMENT '内容摘要',
  `redacted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已脱敏',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_log_chunk_sequence` (`instance_id`, `attempt_id`, `sequence_no`),
  KEY `idx_log_chunk_instance` (`tenant_id`, `instance_id`, `sequence_no`),
  KEY `idx_log_chunk_time` (`tenant_id`, `log_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Mango Job 日志分片';

CREATE TABLE IF NOT EXISTS `mango_job_event` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `job_id` bigint DEFAULT NULL COMMENT 'Mango 任务 ID',
  `instance_id` bigint DEFAULT NULL COMMENT 'Mango 实例 ID',
  `attempt_id` bigint DEFAULT NULL COMMENT '执行尝试 ID',
  `worker_id` bigint DEFAULT NULL COMMENT 'Worker ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_time` datetime NOT NULL COMMENT '事件时间',
  `trace_id` varchar(128) DEFAULT NULL COMMENT '链路追踪 ID',
  `payload` text DEFAULT NULL COMMENT '事件 payload',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_job_time` (`tenant_id`, `job_id`, `event_time`),
  KEY `idx_event_instance_time` (`tenant_id`, `instance_id`, `event_time`),
  KEY `idx_event_type_time` (`tenant_id`, `event_type`, `event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Mango Job 事件';
