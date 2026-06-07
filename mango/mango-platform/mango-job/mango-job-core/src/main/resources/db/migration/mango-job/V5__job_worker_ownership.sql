-- Mango native Job Worker ownership and deployment metadata.

ALTER TABLE `mango_job_definition`
  ADD COLUMN `owner_service` varchar(128) DEFAULT NULL COMMENT '执行服务编码' AFTER `app_code`;

ALTER TABLE `mango_job_definition`
  ADD COLUMN `worker_group` varchar(128) DEFAULT NULL COMMENT 'Worker 分组' AFTER `owner_service`;

UPDATE `mango_job_definition`
SET `owner_service` = `app_code`
WHERE `owner_service` IS NULL OR `owner_service` = '';

UPDATE `mango_job_definition`
SET `worker_group` = `owner_service`
WHERE `worker_group` IS NULL OR `worker_group` = '';

ALTER TABLE `mango_job_definition`
  ADD KEY `idx_job_owner_handler` (`tenant_id`, `owner_service`, `worker_group`, `app_code`, `handler_name`, `status`);

ALTER TABLE `mango_job_worker_snapshot`
  ADD COLUMN `service_code` varchar(128) DEFAULT NULL COMMENT '执行服务编码' AFTER `app_code`;

ALTER TABLE `mango_job_worker_snapshot`
  ADD COLUMN `worker_group` varchar(128) DEFAULT NULL COMMENT 'Worker 分组' AFTER `service_code`;

ALTER TABLE `mango_job_worker_snapshot`
  ADD COLUMN `runtime_address` varchar(256) DEFAULT NULL COMMENT '运行地址' AFTER `worker_address`;

ALTER TABLE `mango_job_worker_snapshot`
  ADD COLUMN `transport_type` varchar(32) DEFAULT NULL COMMENT '通信方式' AFTER `runtime_address`;

ALTER TABLE `mango_job_worker_snapshot`
  ADD COLUMN `register_source` varchar(32) DEFAULT NULL COMMENT '注册来源' AFTER `transport_type`;

ALTER TABLE `mango_job_worker_snapshot`
  ADD COLUMN `instance_id` varchar(128) DEFAULT NULL COMMENT '实例标识' AFTER `register_source`;

UPDATE `mango_job_worker_snapshot`
SET `service_code` = `app_code`
WHERE `service_code` IS NULL OR `service_code` = '';

UPDATE `mango_job_worker_snapshot`
SET `worker_group` = `service_code`
WHERE `worker_group` IS NULL OR `worker_group` = '';

UPDATE `mango_job_worker_snapshot`
SET `runtime_address` = `worker_address`
WHERE `runtime_address` IS NULL OR `runtime_address` = '';

UPDATE `mango_job_worker_snapshot`
SET `transport_type` = CASE
  WHEN `worker_address` LIKE 'in-memory://%' THEN 'IN_MEMORY'
  WHEN `worker_address` LIKE 'http://%' OR `worker_address` LIKE 'https://%' THEN 'HTTP_INTERNAL'
  ELSE NULL
END
WHERE `transport_type` IS NULL OR `transport_type` = '';

UPDATE `mango_job_worker_snapshot`
SET `register_source` = CASE
  WHEN `worker_address` LIKE 'in-memory://%' THEN 'EMBEDDED_AUTO'
  ELSE 'REMOTE_AUTO'
END
WHERE `register_source` IS NULL OR `register_source` = '';

UPDATE `mango_job_worker_snapshot`
SET `instance_id` = `engine_worker_id`
WHERE `instance_id` IS NULL OR `instance_id` = '';

ALTER TABLE `mango_job_worker_snapshot`
  DROP INDEX `uk_worker_tenant_app_engine_address`;

ALTER TABLE `mango_job_worker_snapshot`
  ADD UNIQUE KEY `uk_worker_owner_engine_address` (`tenant_id`, `service_code`, `worker_group`, `engine_type`, `worker_address`);

ALTER TABLE `mango_job_worker_snapshot`
  ADD KEY `idx_worker_owner_status` (`tenant_id`, `service_code`, `worker_group`, `status`, `last_heartbeat_at`);

ALTER TABLE `mango_job_worker_capability`
  ADD COLUMN `service_code` varchar(128) DEFAULT NULL COMMENT '执行服务编码' AFTER `app_code`;

ALTER TABLE `mango_job_worker_capability`
  ADD COLUMN `worker_group` varchar(128) DEFAULT NULL COMMENT 'Worker 分组' AFTER `service_code`;

ALTER TABLE `mango_job_worker_capability`
  ADD COLUMN `job_code` varchar(128) DEFAULT NULL COMMENT '支持的任务编码' AFTER `worker_group`;

UPDATE `mango_job_worker_capability` capability
SET capability.`service_code` = (
      SELECT worker.`service_code`
      FROM `mango_job_worker_snapshot` worker
      WHERE worker.`id` = capability.`worker_id`
    )
WHERE capability.`service_code` IS NULL OR capability.`service_code` = '';

UPDATE `mango_job_worker_capability` capability
SET capability.`worker_group` = (
      SELECT worker.`worker_group`
      FROM `mango_job_worker_snapshot` worker
      WHERE worker.`id` = capability.`worker_id`
    )
WHERE capability.`worker_group` IS NULL OR capability.`worker_group` = '';

UPDATE `mango_job_worker_capability`
SET `job_code` = ''
WHERE `job_code` IS NULL;

ALTER TABLE `mango_job_worker_capability`
  MODIFY COLUMN `job_code` varchar(128) NOT NULL DEFAULT '' COMMENT '支持的任务编码，空串表示不限制任务编码';

ALTER TABLE `mango_job_worker_capability`
  MODIFY COLUMN `handler_name` varchar(128) NOT NULL COMMENT '处理器名称';

ALTER TABLE `mango_job_worker_capability`
  DROP INDEX `uk_worker_handler`;

ALTER TABLE `mango_job_worker_capability`
  ADD UNIQUE KEY `uk_worker_owner_handler` (`worker_id`, `service_code`, `worker_group`, `app_code`, `handler_name`, `job_code`);

ALTER TABLE `mango_job_worker_capability`
  ADD KEY `idx_capability_owner_handler` (`tenant_id`, `service_code`, `worker_group`, `app_code`, `handler_name`, `job_code`, `enabled`);

ALTER TABLE `mango_job_attempt`
  ADD COLUMN `service_code_snapshot` varchar(128) DEFAULT NULL COMMENT '执行服务编码快照' AFTER `worker_address_snapshot`;

ALTER TABLE `mango_job_attempt`
  ADD COLUMN `worker_group_snapshot` varchar(128) DEFAULT NULL COMMENT 'Worker 分组快照' AFTER `service_code_snapshot`;
