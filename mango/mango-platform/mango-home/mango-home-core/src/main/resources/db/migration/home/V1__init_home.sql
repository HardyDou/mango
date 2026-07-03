CREATE TABLE IF NOT EXISTS `sys_user_home_page` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(64) NOT NULL COMMENT '首页名称',
  `layout_json` longtext NOT NULL COMMENT '布局JSON',
  `sort` int NOT NULL DEFAULT '10' COMMENT '排序',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_user_home_page_user` (`tenant_id`, `user_id`, `enabled`, `sort`),
  KEY `idx_sys_user_home_page_org` (`tenant_id`, `org_id`),
  KEY `idx_sys_user_home_page_name` (`tenant_id`, `user_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户首页工作台';

CREATE TABLE IF NOT EXISTS `sys_user_home_preference` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `default_home_page_id` bigint DEFAULT NULL COMMENT '默认首页ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_home_preference_scope` (`tenant_id`, `user_id`),
  KEY `idx_sys_user_home_preference_org` (`tenant_id`, `org_id`),
  KEY `idx_sys_user_home_preference_default` (`default_home_page_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户首页偏好';
