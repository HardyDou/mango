CREATE TABLE IF NOT EXISTS `numgen_generator` (
  `id` bigint NOT NULL COMMENT '主键',
  `gen_key` varchar(128) NOT NULL COMMENT '业务Key',
  `gen_name` varchar(128) NOT NULL COMMENT '名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `current_rule_version` int DEFAULT NULL COMMENT '当前规则版本',
  `current_publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '当前发布状态',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_numgen_generator_tenant_key` (`tenant_id`, `gen_key`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号生成器';

CREATE TABLE IF NOT EXISTS `numgen_rule` (
  `id` bigint NOT NULL COMMENT '主键',
  `gen_key` varchar(128) NOT NULL COMMENT '业务Key',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `version` int NOT NULL DEFAULT '1' COMMENT '规则版本',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0-未生效，1-生效中',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_numgen_rule_tenant_key_version` (`tenant_id`, `gen_key`, `version`, `del_flag`),
  KEY `idx_numgen_rule_tenant_key_status` (`tenant_id`, `gen_key`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号规则';

CREATE TABLE IF NOT EXISTS `numgen_rule_segment` (
  `id` bigint NOT NULL COMMENT '主键',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `sort_order` int NOT NULL COMMENT '排序',
  `segment_type` varchar(32) NOT NULL COMMENT '片段类型',
  `segment_name` varchar(128) NOT NULL COMMENT '片段名称',
  `literal_value` varchar(128) DEFAULT NULL COMMENT '字符串内容',
  `variable_key` varchar(128) DEFAULT NULL COMMENT '变量键',
  `date_format` varchar(64) DEFAULT NULL COMMENT '日期格式',
  `seq_width` int DEFAULT NULL COMMENT '流水位数',
  `pad_char` varchar(1) DEFAULT '0' COMMENT '补齐字符',
  `sequence_scope` tinyint NOT NULL DEFAULT '0' COMMENT '是否参与流水分组：0-否，1-是',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_numgen_rule_segment_rule_order` (`rule_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号规则片段';

CREATE TABLE IF NOT EXISTS `numgen_sequence` (
  `id` bigint NOT NULL COMMENT '主键',
  `gen_key` varchar(128) NOT NULL COMMENT '业务Key',
  `rule_version` int NOT NULL DEFAULT '1' COMMENT '规则版本',
  `scope_key` varchar(256) NOT NULL DEFAULT 'GLOBAL' COMMENT '流水分组键',
  `current_value` bigint NOT NULL DEFAULT '0' COMMENT '当前序列值',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_numgen_sequence_tenant_scope` (`tenant_id`, `gen_key`, `scope_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号生成序列';

CREATE TABLE IF NOT EXISTS `numgen_history` (
  `id` bigint NOT NULL COMMENT '主键',
  `gen_key` varchar(128) NOT NULL COMMENT '业务Key',
  `rule_id` bigint DEFAULT NULL COMMENT '规则ID',
  `result_no` varchar(256) NOT NULL COMMENT '编号结果',
  `rule_version` int NOT NULL COMMENT '规则版本',
  `biz_key` varchar(128) DEFAULT NULL COMMENT '业务键',
  `input_digest` varchar(256) DEFAULT NULL COMMENT '输入摘要',
  `cost_millis` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-失败，1-成功',
  `error_message` varchar(512) DEFAULT NULL COMMENT '错误信息',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_numgen_history_tenant_key_time` (`tenant_id`, `gen_key`, `create_time`),
  KEY `idx_numgen_history_result` (`result_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号生成历史';
