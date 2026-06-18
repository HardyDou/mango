CREATE TABLE IF NOT EXISTS `resource_registry` (
  `id` bigint NOT NULL COMMENT '主键',
  `resource_id` varchar(64) NOT NULL COMMENT '稳定资源ID，雪花算法字符串',
  `resource_version` int NOT NULL COMMENT '资源声明版本',
  `resource_type` varchar(64) NOT NULL COMMENT '资源类型',
  `module_code` varchar(64) NOT NULL COMMENT '声明来源模块',
  `biz_key` varchar(128) NOT NULL COMMENT '业务稳定键',
  `name` varchar(128) DEFAULT NULL COMMENT '资源名称',
  `target_module` varchar(64) NOT NULL COMMENT '目标模块',
  `target_table` varchar(128) DEFAULT NULL COMMENT '目标表',
  `target_id` bigint DEFAULT NULL COMMENT '目标资源ID',
  `source_hash` varchar(64) DEFAULT NULL COMMENT '声明内容Hash',
  `sync_mode` varchar(32) NOT NULL DEFAULT 'AUTO' COMMENT '同步模式',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '资源状态',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_registry_resource_id` (`resource_id`),
  UNIQUE KEY `uk_resource_registry_type_biz_key` (`resource_type`, `biz_key`),
  KEY `idx_resource_registry_module` (`module_code`),
  KEY `idx_resource_registry_target` (`target_module`, `target_table`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源注册中心';

CREATE TABLE IF NOT EXISTS `resource_sync_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `resource_id` bigint DEFAULT NULL COMMENT 'resource_registry.id',
  `sync_type` varchar(32) NOT NULL COMMENT '同步类型',
  `result` varchar(32) NOT NULL COMMENT '同步结果',
  `message` text DEFAULT NULL COMMENT '结果说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_resource_sync_log_resource` (`resource_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源同步日志';

CREATE TABLE IF NOT EXISTS `resource_change_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `resource_id` bigint DEFAULT NULL COMMENT 'resource_registry.id',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID，启动同步为0',
  `before_content` json DEFAULT NULL COMMENT '变更前内容',
  `after_content` json DEFAULT NULL COMMENT '变更后内容',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_resource_change_log_resource` (`resource_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源变更日志';
