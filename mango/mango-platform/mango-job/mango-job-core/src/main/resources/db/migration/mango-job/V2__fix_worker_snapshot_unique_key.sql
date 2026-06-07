-- Fix worker snapshot uniqueness for multi-tenant and multi-app deployments.

ALTER TABLE `mango_job_worker_snapshot`
  DROP INDEX `uk_worker_engine_address`;

ALTER TABLE `mango_job_worker_snapshot`
  ADD UNIQUE KEY `uk_worker_tenant_app_engine_address` (`tenant_id`, `app_code`, `engine_type`, `worker_address`);
