CREATE TABLE IF NOT EXISTS `payment_application` (
  `id` bigint NOT NULL COMMENT '主键',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码',
  `app_name` varchar(128) NOT NULL COMMENT '应用名称',
  `notify_url` varchar(512) DEFAULT NULL COMMENT '业务回调地址',
  `return_url` varchar(512) DEFAULT NULL COMMENT '支付完成跳转地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_application_tenant_code` (`tenant_id`, `app_code`, `del_flag`),
  KEY `idx_payment_application_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付接入应用';

CREATE TABLE IF NOT EXISTS `payment_enterprise_subject` (
  `id` bigint NOT NULL COMMENT '主键',
  `subject_name` varchar(128) NOT NULL COMMENT '主体名称',
  `credit_code` varchar(64) NOT NULL COMMENT '统一社会信用代码',
  `bank_account_no` varchar(64) NOT NULL COMMENT '银行账户',
  `bank_name` varchar(128) NOT NULL COMMENT '开户行',
  `license_file_id` bigint DEFAULT NULL COMMENT '证照文件ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_subject_tenant_credit` (`tenant_id`, `credit_code`, `del_flag`),
  KEY `idx_payment_subject_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付收款主体';

CREATE TABLE IF NOT EXISTS `payment_channel` (
  `id` bigint NOT NULL COMMENT '主键',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `channel_name` varchar(128) NOT NULL COMMENT '通道名称',
  `environment` varchar(32) NOT NULL COMMENT '环境',
  `merchant_no` varchar(64) DEFAULT NULL COMMENT '商户号',
  `gateway_url` varchar(512) DEFAULT NULL COMMENT '通道网关地址',
  `public_key_ref` varchar(256) DEFAULT NULL COMMENT '公钥引用',
  `private_key_ref` varchar(256) DEFAULT NULL COMMENT '私钥引用',
  `cert_file_id` bigint DEFAULT NULL COMMENT '证书文件ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_tenant_code_env_merchant` (`tenant_id`, `channel_code`, `environment`, `merchant_no`, `del_flag`),
  KEY `idx_payment_channel_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道';

CREATE TABLE IF NOT EXISTS `payment_method` (
  `id` bigint NOT NULL COMMENT '主键',
  `method_code` varchar(64) NOT NULL COMMENT '支付方式编码',
  `method_name` varchar(128) NOT NULL COMMENT '支付方式名称',
  `channel_id` bigint NOT NULL COMMENT '默认通道ID',
  `min_amount` bigint DEFAULT NULL COMMENT '单笔最小金额，单位分',
  `max_amount` bigint DEFAULT NULL COMMENT '单笔最大金额，单位分',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_method_tenant_code` (`tenant_id`, `method_code`, `del_flag`),
  KEY `idx_payment_method_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式';

CREATE TABLE IF NOT EXISTS `payment_cashier_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `cashier_name` varchar(128) NOT NULL COMMENT '收银台名称',
  `application_id` bigint NOT NULL COMMENT '适用应用ID',
  `enterprise_subject_id` bigint NOT NULL COMMENT '适用主体ID',
  `terminal_type` varchar(32) NOT NULL COMMENT '终端类型',
  `method_ids` varchar(512) DEFAULT NULL COMMENT '可见支付方式ID',
  `default_method_id` bigint DEFAULT NULL COMMENT '默认支付方式ID',
  `expire_minutes` int NOT NULL COMMENT '支付超时分钟数',
  `result_return_url` varchar(512) DEFAULT NULL COMMENT '结果跳转地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_payment_cashier_tenant_app` (`tenant_id`, `application_id`, `terminal_type`),
  KEY `idx_payment_cashier_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收银台配置';

CREATE TABLE IF NOT EXISTS `payment_business_order` (
  `id` bigint NOT NULL COMMENT '主键',
  `biz_order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码',
  `subject_id` bigint NOT NULL COMMENT '收款主体ID',
  `amount` bigint NOT NULL COMMENT '订单金额，单位分',
  `currency` varchar(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `status` varchar(32) NOT NULL COMMENT '业务订单状态',
  `expire_time` datetime DEFAULT NULL COMMENT '订单过期时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_business_order_tenant_app_no` (`tenant_id`, `app_code`, `biz_order_no`),
  KEY `idx_payment_business_order_tenant_status` (`tenant_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付业务订单';

CREATE TABLE IF NOT EXISTS `payment_order` (
  `id` bigint NOT NULL COMMENT '主键',
  `pay_order_no` varchar(64) NOT NULL COMMENT '支付订单号',
  `business_order_id` bigint NOT NULL COMMENT '业务订单ID',
  `channel_id` bigint NOT NULL COMMENT '通道ID',
  `method_id` bigint NOT NULL COMMENT '支付方式ID',
  `amount` bigint NOT NULL COMMENT '支付金额，单位分',
  `status` varchar(32) NOT NULL COMMENT '支付订单状态',
  `channel_trade_no` varchar(128) DEFAULT NULL COMMENT '通道交易号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_order_no` (`pay_order_no`),
  KEY `idx_payment_order_business` (`business_order_id`),
  KEY `idx_payment_order_tenant_status` (`tenant_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单';

CREATE TABLE IF NOT EXISTS `payment_refund_order` (
  `id` bigint NOT NULL COMMENT '主键',
  `refund_order_no` varchar(64) NOT NULL COMMENT '退款订单号',
  `biz_refund_no` varchar(64) NOT NULL COMMENT '业务退款号',
  `payment_order_id` bigint NOT NULL COMMENT '支付订单ID',
  `refund_amount` bigint NOT NULL COMMENT '退款金额，单位分',
  `status` varchar(32) NOT NULL COMMENT '退款状态',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_refund_tenant_biz` (`tenant_id`, `biz_refund_no`),
  UNIQUE KEY `uk_payment_refund_no` (`refund_order_no`),
  KEY `idx_payment_refund_payment` (`payment_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款订单';

CREATE TABLE IF NOT EXISTS `payment_transaction_flow` (
  `id` bigint NOT NULL COMMENT '主键',
  `flow_no` varchar(64) NOT NULL COMMENT '流水号',
  `business_order_id` bigint DEFAULT NULL COMMENT '业务订单ID',
  `payment_order_id` bigint DEFAULT NULL COMMENT '支付订单ID',
  `refund_order_id` bigint DEFAULT NULL COMMENT '退款订单ID',
  `flow_type` varchar(32) NOT NULL COMMENT '流水类型',
  `amount` bigint NOT NULL COMMENT '金额，单位分',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_flow_no` (`flow_no`),
  KEY `idx_payment_flow_tenant_time` (`tenant_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易流水';

CREATE TABLE IF NOT EXISTS `payment_exception_order` (
  `id` bigint NOT NULL COMMENT '主键',
  `exception_no` varchar(64) NOT NULL COMMENT '异常单号',
  `related_order_no` varchar(64) NOT NULL COMMENT '关联订单号',
  `exception_type` varchar(64) NOT NULL COMMENT '异常类型',
  `severity` varchar(32) NOT NULL COMMENT '异常级别',
  `handle_status` varchar(32) NOT NULL COMMENT '处理状态',
  `reason` varchar(512) DEFAULT NULL COMMENT '异常原因',
  `handle_result` varchar(512) DEFAULT NULL COMMENT '处理结果',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_exception_no` (`exception_no`),
  KEY `idx_payment_exception_tenant_status` (`tenant_id`, `handle_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付异常订单';

CREATE TABLE IF NOT EXISTS `payment_notification_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `notification_no` varchar(64) NOT NULL COMMENT '通知单号',
  `related_order_no` varchar(64) NOT NULL COMMENT '关联订单号',
  `notification_type` varchar(64) NOT NULL COMMENT '通知类型',
  `target_url` varchar(512) NOT NULL COMMENT '通知目标地址',
  `notify_status` varchar(32) NOT NULL COMMENT '通知状态',
  `retry_times` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下一次重试时间',
  `response_code` varchar(64) DEFAULT NULL COMMENT '响应码',
  `response_message` varchar(512) DEFAULT NULL COMMENT '响应信息',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_notification_no` (`notification_no`),
  KEY `idx_payment_notification_tenant_status` (`tenant_id`, `notify_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通知记录';

CREATE TABLE IF NOT EXISTS `payment_reconciliation` (
  `id` bigint NOT NULL COMMENT '主键',
  `reconciliation_no` varchar(64) NOT NULL COMMENT '对账批次号',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `bill_date` date NOT NULL COMMENT '账单日期',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '账单笔数',
  `total_amount` bigint NOT NULL DEFAULT '0' COMMENT '账单金额，单位分',
  `match_status` varchar(32) NOT NULL COMMENT '匹配状态',
  `bill_file_id` bigint DEFAULT NULL COMMENT '账单文件ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_reconciliation_no` (`reconciliation_no`),
  KEY `idx_payment_reconciliation_tenant_bill` (`tenant_id`, `channel_code`, `bill_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付对账批次';

CREATE TABLE IF NOT EXISTS `payment_difference` (
  `id` bigint NOT NULL COMMENT '主键',
  `difference_no` varchar(64) NOT NULL COMMENT '差异单号',
  `reconciliation_id` bigint NOT NULL COMMENT '对账批次ID',
  `related_order_no` varchar(64) NOT NULL COMMENT '关联订单号',
  `difference_type` varchar(64) NOT NULL COMMENT '差异类型',
  `difference_amount` bigint NOT NULL DEFAULT '0' COMMENT '差异金额，单位分',
  `process_status` varchar(32) NOT NULL COMMENT '处理状态',
  `process_result` varchar(512) DEFAULT NULL COMMENT '处理结果',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_difference_no` (`difference_no`),
  KEY `idx_payment_difference_tenant_status` (`tenant_id`, `process_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付对账差异';

CREATE TABLE IF NOT EXISTS `payment_settlement_summary` (
  `id` bigint NOT NULL COMMENT '主键',
  `settlement_date` date NOT NULL COMMENT '结算日期',
  `enterprise_subject_id` bigint NOT NULL COMMENT '企业主体ID',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `trade_amount` bigint NOT NULL DEFAULT '0' COMMENT '交易金额，单位分',
  `refund_amount` bigint NOT NULL DEFAULT '0' COMMENT '退款金额，单位分',
  `fee_amount` bigint NOT NULL DEFAULT '0' COMMENT '手续费金额，单位分',
  `net_amount` bigint NOT NULL DEFAULT '0' COMMENT '净结算金额，单位分',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_settlement_summary` (`tenant_id`, `settlement_date`, `enterprise_subject_id`, `channel_code`),
  KEY `idx_payment_settlement_tenant_date` (`tenant_id`, `settlement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付结算汇总';

CREATE TABLE IF NOT EXISTS `payment_operation_audit` (
  `id` bigint NOT NULL COMMENT '主键',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人名称',
  `operation_action` varchar(64) NOT NULL COMMENT '操作动作',
  `resource_type` varchar(64) NOT NULL COMMENT '资源类型',
  `resource_id` varchar(64) DEFAULT NULL COMMENT '资源ID',
  `operation_result` varchar(32) NOT NULL COMMENT '操作结果',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_payment_audit_tenant_time` (`tenant_id`, `operation_time`),
  KEY `idx_payment_audit_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付操作审计';

CREATE TABLE IF NOT EXISTS `payment_virtual_channel_payment` (
  `id` bigint NOT NULL COMMENT '主键',
  `virtual_payment_no` varchar(64) NOT NULL COMMENT '内置虚拟通道支付单号',
  `cashier_config_id` bigint NOT NULL COMMENT '收银台配置ID',
  `payment_method_id` bigint DEFAULT NULL COMMENT '支付方式ID',
  `title` varchar(128) NOT NULL COMMENT '付款标题',
  `amount` bigint NOT NULL COMMENT '付款金额，单位分',
  `payer_name` varchar(128) DEFAULT NULL COMMENT '付款人',
  `status` varchar(32) NOT NULL COMMENT '支付状态',
  `paid_time` datetime DEFAULT NULL COMMENT '完成时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_virtual_payment_no` (`virtual_payment_no`),
  KEY `idx_payment_virtual_tenant_cashier` (`tenant_id`, `cashier_config_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内置虚拟通道支付记录';
