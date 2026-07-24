-- Stable channel identity, split Secret storage and explicit routing modes.

-- V1 is kept as the canonical fresh-install schema. These guards make V2 a no-op for
-- fresh databases while still upgrading the V1 already published in older artifacts.
SET @notice_channel_columns_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notice_channel_config'
      AND column_name = 'config_code'
  ),
  'DO 0',
  'ALTER TABLE `notice_channel_config`
    ADD COLUMN `config_code` varchar(64) DEFAULT NULL COMMENT ''渠道配置稳定编码'' AFTER `id`,
    ADD COLUMN `secret_refs_json` text COMMENT ''Resource Secret 引用 JSON'' AFTER `config_json`,
    ADD COLUMN `secret_config_json` text COMMENT ''环境人工补录 Secret JSON'' AFTER `secret_refs_json`,
    ADD COLUMN `resource_id` varchar(128) DEFAULT NULL COMMENT ''Resource 声明 ID'' AFTER `secret_config_json`,
    ADD COLUMN `resource_version` int DEFAULT NULL COMMENT ''Resource 版本'' AFTER `resource_id`,
    ADD COLUMN `resource_module_code` varchar(64) DEFAULT NULL COMMENT ''Resource 模块编码'' AFTER `resource_version`,
    ADD COLUMN `resource_source` varchar(32) NOT NULL DEFAULT ''MANUAL'' COMMENT ''配置来源'' AFTER `resource_module_code`,
    ADD COLUMN `managed_fields_json` text COMMENT ''Resource 管理字段清单'' AFTER `resource_source`,
    ADD COLUMN `secret_status` varchar(32) NOT NULL DEFAULT ''NOT_REQUIRED'' COMMENT ''Secret 完整性状态'' AFTER `managed_fields_json`'
);
PREPARE notice_channel_columns_stmt FROM @notice_channel_columns_ddl;
EXECUTE notice_channel_columns_stmt;
DEALLOCATE PREPARE notice_channel_columns_stmt;

UPDATE `notice_channel_config`
SET `config_code` = CONCAT('LEGACY_', `id`)
WHERE `config_code` IS NULL OR `config_code` = '';

SET @notice_channel_indexes_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notice_channel_config'
      AND index_name = 'uk_notice_channel_config_code'
  ),
  'DO 0',
  'ALTER TABLE `notice_channel_config`
    MODIFY COLUMN `config_code` varchar(64) NOT NULL COMMENT ''渠道配置稳定编码'',
    ADD UNIQUE KEY `uk_notice_channel_config_code` (`tenant_id`,`config_code`),
    ADD KEY `idx_notice_channel_source` (`tenant_id`,`resource_source`,`resource_id`)'
);
PREPARE notice_channel_indexes_stmt FROM @notice_channel_indexes_ddl;
EXECUTE notice_channel_indexes_stmt;
DEALLOCATE PREPARE notice_channel_indexes_stmt;

SET @notice_template_route_ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notice_business_channel_template'
      AND column_name = 'route_mode'
  ),
  'DO 0',
  'ALTER TABLE `notice_business_channel_template`
    ADD COLUMN `route_mode` varchar(16) NOT NULL DEFAULT ''AUTO'' COMMENT ''路由模式：EXACT、TAG、AUTO'' AFTER `channel_config_id`,
    ADD COLUMN `route_tag_code` varchar(64) DEFAULT NULL COMMENT ''TAG 模式标签编码'' AFTER `route_mode`'
);
PREPARE notice_template_route_stmt FROM @notice_template_route_ddl;
EXECUTE notice_template_route_stmt;
DEALLOCATE PREPARE notice_template_route_stmt;

UPDATE `notice_business_channel_template`
SET `route_mode` = CASE WHEN `channel_config_id` IS NULL THEN 'AUTO' ELSE 'EXACT' END;

CREATE TABLE IF NOT EXISTS `notice_channel_route_tag` (
  `id` bigint NOT NULL COMMENT '主键',
  `channel_type` varchar(32) NOT NULL COMMENT '渠道类型',
  `tag_code` varchar(64) NOT NULL COMMENT '标签稳定编码',
  `tag_name` varchar(128) NOT NULL COMMENT '标签名称',
  `description` varchar(500) DEFAULT NULL COMMENT '标签说明',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_channel_route_tag` (`tenant_id`,`channel_type`,`tag_code`),
  KEY `idx_notice_channel_route_tag_name` (`tenant_id`,`channel_type`,`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知渠道路由标签表';

CREATE TABLE IF NOT EXISTS `notice_channel_config_route_tag` (
  `id` bigint NOT NULL COMMENT '主键',
  `channel_config_id` bigint NOT NULL COMMENT '渠道配置 ID',
  `route_tag_id` bigint NOT NULL COMMENT '路由标签 ID',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_channel_config_route_tag` (`tenant_id`,`channel_config_id`,`route_tag_id`),
  KEY `idx_notice_channel_route_tag_config` (`tenant_id`,`route_tag_id`,`channel_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知渠道配置路由标签关系表';
