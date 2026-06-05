CREATE TABLE IF NOT EXISTS `notice_wecom_sync_mapping` (
  `id` bigint NOT NULL COMMENT '主键',
  `sync_type` varchar(32) NOT NULL COMMENT '同步对象类型：DEPARTMENT/USER',
  `external_id` varchar(128) NOT NULL COMMENT '企业微信外部ID',
  `local_id` bigint NOT NULL COMMENT 'Mango本地ID',
  `data_hash` varchar(64) DEFAULT NULL COMMENT '同步数据指纹',
  `display_name` varchar(128) DEFAULT NULL COMMENT '显示名称快照',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_wecom_sync_external` (`tenant_id`, `sync_type`, `external_id`),
  KEY `idx_notice_wecom_sync_local` (`tenant_id`, `sync_type`, `local_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业微信同步映射表';
