-- Mango information collection canonical schema.
CREATE TABLE IF NOT EXISTS `information_form` (
  `id` bigint NOT NULL COMMENT '表单ID',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `form_code` varchar(64) NOT NULL COMMENT '表单编码',
  `form_name` varchar(128) NOT NULL COMMENT '表单名称',
  `description` varchar(1000) DEFAULT NULL COMMENT '表单说明',
  `definition_json` longtext NOT NULL COMMENT '字段定义JSON',
  `success_message` varchar(255) DEFAULT NULL COMMENT '成功提示',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '表单状态',
  `server_secret` varchar(128) NOT NULL COMMENT '服务端接入密钥',
  `server_secret_digest` varchar(128) NOT NULL COMMENT '服务端接入密钥摘要',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_information_form_tenant_code` (`tenant_id`,`form_code`),
  KEY `idx_information_form_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信息采集表单';

CREATE TABLE IF NOT EXISTS `information_submission` (
  `id` bigint NOT NULL COMMENT '提交记录ID',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `form_id` bigint NOT NULL COMMENT '表单ID',
  `form_code` varchar(64) NOT NULL COMMENT '表单编码',
  `values_json` longtext NOT NULL COMMENT '提交值JSON',
  `source_page` varchar(500) DEFAULT NULL COMMENT '来源页面',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
  `process_remark` varchar(1000) DEFAULT NULL COMMENT '处理备注',
  `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_information_submission_idempotency` (`tenant_id`,`form_id`,`idempotency_key`),
  KEY `idx_information_submission_form` (`tenant_id`,`form_id`),
  KEY `idx_information_submission_status` (`tenant_id`,`status`),
  KEY `idx_information_submission_time` (`tenant_id`,`submitted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信息采集提交记录';

CREATE TABLE IF NOT EXISTS `information_submission_snapshot` (
  `id` bigint NOT NULL COMMENT '快照ID',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `submission_id` bigint NOT NULL COMMENT '提交记录ID',
  `form_id` bigint NOT NULL COMMENT '表单ID',
  `definition_json` longtext NOT NULL COMMENT '提交时字段定义JSON',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_information_snapshot_submission` (`submission_id`),
  KEY `idx_information_snapshot_form` (`tenant_id`,`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信息采集字段定义快照';
