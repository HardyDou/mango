CREATE TABLE IF NOT EXISTS `tenant_member_lifecycle_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织标识',
  `user_id` bigint NOT NULL COMMENT '全局账号ID',
  `member_id` bigint NOT NULL COMMENT '成员ID',
  `event_type` varchar(16) NOT NULL COMMENT '事件类型: CREATED, REMOVED, RESTORED',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `occurred_at` datetime NOT NULL COMMENT '事件发生时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_lifecycle_member_time` (`tenant_id`,`member_id`,`occurred_at`),
  KEY `idx_member_lifecycle_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户成员生命周期记录表';
