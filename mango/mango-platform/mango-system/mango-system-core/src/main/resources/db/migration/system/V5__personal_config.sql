CREATE TABLE IF NOT EXISTS `sys_personal_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `group_code` varchar(64) NOT NULL COMMENT '配置分组',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text NOT NULL COMMENT '配置值',
  `value_type` varchar(20) NOT NULL DEFAULT 'JSON' COMMENT '值类型：JSON/STRING/NUMBER/BOOLEAN',
  `config_name` varchar(100) DEFAULT NULL COMMENT '配置名称',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_personal_config_scope` (`tenant_id`, `user_id`, `group_code`, `biz_type`, `config_key`),
  KEY `idx_sys_personal_config_user_group` (`tenant_id`, `user_id`, `group_code`, `biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='个人参数配置表';
