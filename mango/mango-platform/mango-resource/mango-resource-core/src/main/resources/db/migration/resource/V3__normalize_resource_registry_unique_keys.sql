ALTER TABLE `resource_registry`
  DROP INDEX `uk_resource_registry_resource_id`,
  DROP INDEX `uk_resource_registry_type_biz_key`,
  ADD UNIQUE KEY `uk_resource_registry_resource_id` (`resource_id`),
  ADD UNIQUE KEY `uk_resource_registry_type_biz_key` (`resource_type`, `biz_key`);
