-- Organization and post codes are tenant-scoped business identifiers.
ALTER TABLE `sys_org`
  DROP INDEX `uk_sys_org_code`,
  ADD UNIQUE KEY `uk_sys_org_tenant_code` (`tenant_id`, `org_code`);

ALTER TABLE `org_post`
  DROP INDEX `uk_org_post_code`,
  ADD UNIQUE KEY `uk_org_post_tenant_code` (`tenant_id`, `post_code`);
