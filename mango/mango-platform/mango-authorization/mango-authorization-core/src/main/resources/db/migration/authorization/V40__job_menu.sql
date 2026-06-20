INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (9, 'internal-admin', 'mango-job', '任务调度模块', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `module_code` = VALUES(`module_code`),
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (9, 'internal-admin', 'mango-job', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19, 'internal-admin', 'mango-job', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (29, 'internal-admin', 'mango-job', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;
