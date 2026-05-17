CREATE TABLE IF NOT EXISTS `frontend_module_runtime_strategy` (
  `id` bigint NOT NULL COMMENT '模块运行策略ID',
  `app_code` varchar(64) NOT NULL COMMENT '逻辑应用编码',
  `module_code` varchar(128) NOT NULL COMMENT '能力模块编码',
  `deploy_profile` varchar(32) NOT NULL DEFAULT 'monolith' COMMENT '部署配置档: monolith/hybrid/micro',
  `page_type` varchar(32) NOT NULL DEFAULT 'LOCAL_ROUTE' COMMENT '页面运行类型: LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK',
  `runtime_code` varchar(64) NOT NULL COMMENT '前端运行单元编码，关联 frontend_app_registry.app_code',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_frontend_module_runtime_strategy` (`app_code`,`module_code`,`deploy_profile`),
  KEY `idx_frontend_module_runtime_strategy_app` (`app_code`),
  KEY `idx_frontend_module_runtime_strategy_runtime` (`runtime_code`),
  KEY `idx_frontend_module_runtime_strategy_profile` (`deploy_profile`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端模块运行策略表';

INSERT INTO `frontend_app_registry`
  (`id`, `app_code`, `app_type`, `deploy_mode`, `entry_url`, `mount_path`, `active_rule`, `framework`, `version`, `sandbox_enabled`, `style_isolation`, `create_time`, `update_time`)
VALUES
  (1001, 'mango-admin-local', 'LOCAL', 'EMBEDDED', NULL, '/', '/**', 'vue3', 'dev', 0, 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1002, 'mango-admin-rbac-app', 'MICRO_APP', 'REMOTE', 'http://127.0.0.1:5181/src/micro.ts', '/micro/rbac', '/system/**', 'vue3', 'dev', 0, 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `app_type` = VALUES(`app_type`),
  `deploy_mode` = VALUES(`deploy_mode`),
  `entry_url` = VALUES(`entry_url`),
  `mount_path` = VALUES(`mount_path`),
  `active_rule` = VALUES(`active_rule`),
  `framework` = VALUES(`framework`),
  `version` = VALUES(`version`),
  `sandbox_enabled` = VALUES(`sandbox_enabled`),
  `style_isolation` = VALUES(`style_isolation`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (1, 'internal-admin', 'mango-authorization', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'internal-admin', 'mango-system', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'internal-admin', 'mango-workflow', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (11, 'internal-admin', 'mango-authorization', 'hybrid', 'MICRO_ROUTE', 'mango-admin-rbac-app', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (12, 'internal-admin', 'mango-system', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (13, 'internal-admin', 'mango-workflow', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (21, 'internal-admin', 'mango-authorization', 'micro', 'MICRO_ROUTE', 'mango-admin-rbac-app', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (22, 'internal-admin', 'mango-system', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (23, 'internal-admin', 'mango-workflow', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;
