-- Restore module registry rows overwritten by historical V39/V40 id collisions.

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

UPDATE `authorization_app_module`
SET `module_name` = '业务域模块',
    `status` = 1,
    `sort` = 8,
    `update_time` = CURRENT_TIMESTAMP
WHERE `app_code` = 'internal-admin'
  AND `module_code` = 'mango-domain';

SET @job_module_id := (
  SELECT `id`
  FROM `authorization_app_module`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-job'
  LIMIT 1
);

SET @job_module_id := COALESCE(@job_module_id, (
  SELECT COALESCE(MAX(`id`), 0) + 1
  FROM `authorization_app_module`
));

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (@job_module_id, 'internal-admin', 'mango-job', '任务调度模块', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

UPDATE `authorization_app_module`
SET `module_name` = '任务调度模块',
    `status` = 1,
    `sort` = 9,
    `update_time` = CURRENT_TIMESTAMP
WHERE `app_code` = 'internal-admin'
  AND `module_code` = 'mango-job';

SET @strategy_base_id := (
  SELECT COALESCE(MAX(`id`), 0)
  FROM `frontend_module_runtime_strategy`
);

SET @domain_monolith_strategy_id := (
  SELECT `id`
  FROM `frontend_module_runtime_strategy`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-domain'
    AND `deploy_profile` = 'monolith'
  LIMIT 1
);
SET @domain_hybrid_strategy_id := (
  SELECT `id`
  FROM `frontend_module_runtime_strategy`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-domain'
    AND `deploy_profile` = 'hybrid'
  LIMIT 1
);
SET @domain_micro_strategy_id := (
  SELECT `id`
  FROM `frontend_module_runtime_strategy`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-domain'
    AND `deploy_profile` = 'micro'
  LIMIT 1
);
SET @job_monolith_strategy_id := (
  SELECT `id`
  FROM `frontend_module_runtime_strategy`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-job'
    AND `deploy_profile` = 'monolith'
  LIMIT 1
);
SET @job_hybrid_strategy_id := (
  SELECT `id`
  FROM `frontend_module_runtime_strategy`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-job'
    AND `deploy_profile` = 'hybrid'
  LIMIT 1
);
SET @job_micro_strategy_id := (
  SELECT `id`
  FROM `frontend_module_runtime_strategy`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-job'
    AND `deploy_profile` = 'micro'
  LIMIT 1
);

INSERT INTO `frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (COALESCE(@domain_monolith_strategy_id, @strategy_base_id + 1), 'internal-admin', 'mango-domain', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (COALESCE(@domain_hybrid_strategy_id, @strategy_base_id + 2), 'internal-admin', 'mango-domain', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (COALESCE(@domain_micro_strategy_id, @strategy_base_id + 3), 'internal-admin', 'mango-domain', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (COALESCE(@job_monolith_strategy_id, @strategy_base_id + 4), 'internal-admin', 'mango-job', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (COALESCE(@job_hybrid_strategy_id, @strategy_base_id + 5), 'internal-admin', 'mango-job', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (COALESCE(@job_micro_strategy_id, @strategy_base_id + 6), 'internal-admin', 'mango-job', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;
