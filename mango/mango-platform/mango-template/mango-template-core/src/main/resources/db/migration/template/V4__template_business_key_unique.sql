ALTER TABLE `template`
  ADD UNIQUE KEY `uk_template_tenant_business_key` (`tenant_id`, `business_key`);
