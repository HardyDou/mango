ALTER TABLE `resource_registry`
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人 ID' AFTER `last_sync_time`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID' AFTER `created_at`;
