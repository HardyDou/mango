ALTER TABLE `ai_provider_connection`
  DROP COLUMN `last_model_sync_at`;

ALTER TABLE `ai_model`
  DROP INDEX `idx_ai_model_tenant_source_status`,
  DROP COLUMN `catalog_source`,
  DROP COLUMN `remote_status`,
  DROP COLUMN `last_synced_at`;
