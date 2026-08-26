CREATE TABLE IF NOT EXISTS `ai_provider_connection` (
  `id` bigint NOT NULL,
  `code` varchar(64) NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `provider_type` varchar(32) NOT NULL,
  `base_url` varchar(255) NOT NULL,
  `api_key_ciphertext` text NOT NULL,
  `api_key_hint` varchar(8) NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `tenant_id` varchar(64) NOT NULL,
  `org_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_provider_tenant_code` (`tenant_id`, `code`),
  KEY `idx_ai_provider_tenant_status` (`tenant_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户级 AI 厂商接入配置';

CREATE TABLE IF NOT EXISTS `ai_model` (
  `id` bigint NOT NULL,
  `provider_connection_id` bigint NOT NULL,
  `model_name` varchar(128) NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `platform_alias` varchar(128) DEFAULT NULL,
  `capabilities_json` varchar(512) NOT NULL,
  `input_modalities_json` varchar(255) NOT NULL,
  `output_modalities_json` varchar(255) NOT NULL,
  `parameter_json` text DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `tenant_id` varchar(64) NOT NULL,
  `org_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_tenant_provider_name` (`tenant_id`, `provider_connection_id`, `model_name`),
  KEY `idx_ai_model_tenant_provider` (`tenant_id`, `provider_connection_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户级 AI 供应商模型目录';

CREATE TABLE IF NOT EXISTS `ai_capability_route` (
  `id` bigint NOT NULL,
  `capability` varchar(32) NOT NULL,
  `model_id` bigint NOT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `org_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_route_tenant_capability` (`tenant_id`, `capability`),
  KEY `idx_ai_route_tenant_model` (`tenant_id`, `model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户级 AI 能力默认路由';
