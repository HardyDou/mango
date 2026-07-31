ALTER TABLE `file_object`
  DROP INDEX `uk_file_object_hash_storage`,
  DROP INDEX `idx_file_object_location`,
  ADD UNIQUE KEY `uk_file_object_location` (`storage_config_id`, `bucket_name`, `object_name`),
  ADD KEY `idx_file_object_hash_storage` (`storage_config_id`, `bucket_name`, `file_hash`, `file_size`);
