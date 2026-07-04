ALTER TABLE `mango_job_definition`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_instance`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_log_index`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_worker_snapshot`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_alarm_rule`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_engine_mapping`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_operation_log`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_schedule_cursor`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_attempt`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_worker_capability`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_log_chunk`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';

ALTER TABLE `mango_job_event`
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '所属组织ID';
