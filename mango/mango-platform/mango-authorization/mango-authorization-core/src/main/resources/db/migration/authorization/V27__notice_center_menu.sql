-- REBASE_REQUIRED(issue-204): targets authorization_frontend_module_runtime_strategy after table namespace rebase.
-- Databases that already applied the previous local migration set must be rebuilt.

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (8, 'internal-admin', 'mango-notice', '通知中心模块', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `module_code` = VALUES(`module_code`),
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (8, 'internal-admin', 'mango-notice', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18, 'internal-admin', 'mango-notice', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (28, 'internal-admin', 'mango-notice', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

-- Notice menus, button permissions, menu runtime config, package bindings and
-- default role bindings are registered by mango-notice-starter AUTH_MENU resource.
