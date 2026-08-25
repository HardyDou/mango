ALTER TABLE `ai_provider_connection`
  ADD COLUMN `last_model_sync_at` datetime DEFAULT NULL AFTER `enabled`;

ALTER TABLE `ai_model`
  ADD COLUMN `catalog_source` varchar(16) NOT NULL DEFAULT 'MANUAL' AFTER `parameter_json`,
  ADD COLUMN `remote_status` varchar(16) NOT NULL DEFAULT 'ACTIVE' AFTER `catalog_source`,
  ADD COLUMN `last_synced_at` datetime DEFAULT NULL AFTER `remote_status`,
  ADD KEY `idx_ai_model_tenant_source_status` (`tenant_id`, `catalog_source`, `remote_status`);
