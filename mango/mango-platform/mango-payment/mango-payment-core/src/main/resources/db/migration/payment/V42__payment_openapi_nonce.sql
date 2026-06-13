CREATE TABLE IF NOT EXISTS `payment_openapi_nonce` (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` varchar(64) NOT NULL COMMENT '支付应用 AppId',
  `nonce` varchar(128) NOT NULL COMMENT '开放接口随机串',
  `expire_time` datetime NOT NULL COMMENT 'nonce 过期时间',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_openapi_nonce` (`tenant_id`, `app_id`, `nonce`, `del_flag`),
  KEY `idx_payment_openapi_nonce_expire` (`tenant_id`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付开放接口 nonce 防重放';
