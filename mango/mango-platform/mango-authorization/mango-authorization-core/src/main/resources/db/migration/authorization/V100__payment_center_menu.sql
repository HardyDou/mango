-- REBASE_REQUIRED(issue-204): targets authorization_frontend_module_runtime_strategy after table namespace rebase.
-- Databases that already applied the previous local migration set must be rebuilt.

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (6, 'internal-admin', 'mango-payment', '支付中心', 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (6, 'internal-admin', 'mango-payment', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (16, 'internal-admin', 'mango-payment', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (26, 'internal-admin', 'mango-payment', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

-- Payment menus, button permissions, menu runtime config, package bindings and default role bindings are registered by mango-payment-starter AUTH_MENU resource payment-common-menu.json.
