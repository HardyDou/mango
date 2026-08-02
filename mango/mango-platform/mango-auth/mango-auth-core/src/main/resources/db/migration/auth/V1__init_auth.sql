CREATE TABLE IF NOT EXISTS `auth_provider_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户标识',
  `org_id` bigint DEFAULT NULL COMMENT '组织标识',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码',
  `provider` varchar(32) NOT NULL COMMENT '第三方身份提供方',
  `client_id` varchar(128) NOT NULL COMMENT '厂商客户端标识',
  `provider_tenant_id` varchar(128) DEFAULT NULL COMMENT '厂商租户标识',
  `agent_id` varchar(64) DEFAULT NULL COMMENT '厂商应用 AgentId',
  `secret_ciphertext` varchar(2048) NOT NULL COMMENT '加密后的客户端密钥',
  `redirect_uris_json` text NOT NULL COMMENT '允许的回调地址 JSON',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX `uk_auth_provider_config_scope`
  ON `auth_provider_config` (`tenant_id`, `app_code`, `provider`);

CREATE INDEX `idx_auth_provider_config_discovery`
  ON `auth_provider_config` (`tenant_id`, `app_code`, `enabled`);
