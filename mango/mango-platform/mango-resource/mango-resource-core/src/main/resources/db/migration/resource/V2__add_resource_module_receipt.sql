CREATE TABLE IF NOT EXISTS `resource_module_receipt` (
  `environment_key` varchar(128) NOT NULL COMMENT 'Bootstrap环境标识',
  `app_code` varchar(128) NOT NULL COMMENT '来源应用',
  `service_code` varchar(128) NOT NULL COMMENT '来源服务',
  `module_code` varchar(64) NOT NULL COMMENT '资源模块编码',
  `module_hash` varchar(64) NOT NULL COMMENT '模块完整期望状态SHA-256',
  `generation` bigint NOT NULL COMMENT '最后成功协调的generation',
  `manifest_fingerprint` varchar(64) NOT NULL COMMENT '所属发布清单指纹',
  `state` varchar(32) NOT NULL COMMENT 'EXPANDED或FINALIZED',
  `declaration_count` int NOT NULL DEFAULT 0 COMMENT '模块声明数量',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`environment_key`, `app_code`, `service_code`, `module_code`),
  KEY `idx_resource_module_receipt_generation` (`environment_key`, `generation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resource模块安装回执';
