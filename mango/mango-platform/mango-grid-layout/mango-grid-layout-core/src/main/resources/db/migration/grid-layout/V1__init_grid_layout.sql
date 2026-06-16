CREATE TABLE IF NOT EXISTS `mango_user_grid_layout` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `page_code` varchar(100) NOT NULL COMMENT '页面编码',
  `schema_version` int NOT NULL DEFAULT '1' COMMENT '布局结构版本',
  `layout_json` longtext NOT NULL COMMENT '布局JSON',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mango_user_grid_layout_scope` (`tenant_id`, `user_id`, `page_code`),
  KEY `idx_mango_user_grid_layout_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户自定义栅格布局';
