-- REBASE_REQUIRED(issue-204): targets authorization_frontend_module_runtime_strategy after table namespace rebase.
-- Databases that already applied the previous local migration set must be rebuilt.

-- Register business domain module runtime metadata.
-- Business domain menus are registered by mango-domain-starter AUTH_MENU resource.

SET @domain_module_id := (
  SELECT `id`
  FROM `authorization_app_module`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-domain'
  LIMIT 1
);

SET @domain_module_id := COALESCE(@domain_module_id, (
  SELECT COALESCE(MAX(`id`), 0) + 1
  FROM `authorization_app_module`
));

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (@domain_module_id, 'internal-admin', 'mango-domain', '业务域模块', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (9, 'internal-admin', 'mango-domain', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19, 'internal-admin', 'mango-domain', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (29, 'internal-admin', 'mango-domain', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;
