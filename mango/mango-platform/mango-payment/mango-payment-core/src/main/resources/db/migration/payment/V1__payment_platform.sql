-- Payment platform clean-database baseline.
-- This unpublished module supports fresh databases only; do not apply V1 over a V3-V102 Flyway history.
-- Runtime orders, flows, reconciliation records, fixture records, and merchant secrets are intentionally excluded.

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_application` (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` varchar(64) NOT NULL COMMENT 'AppId，业务系统调用支付平台的应用身份',
  `app_name` varchar(128) NOT NULL COMMENT '应用名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `app_secret` varchar(512) DEFAULT NULL COMMENT '应用密钥密文',
  `sign_algorithm` varchar(32) DEFAULT NULL COMMENT '签名算法',
  `ip_whitelist` varchar(1024) DEFAULT NULL COMMENT 'IP 白名单',
  `payload_encrypt_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '报文加密开关',
  `secret_configured` tinyint NOT NULL DEFAULT '0' COMMENT '应用密钥是否已配置',
  `secret_version` int NOT NULL DEFAULT '0' COMMENT '应用密钥版本',
  `secret_last_reset_time` datetime DEFAULT NULL COMMENT '应用密钥最后重置时间',
  `notify_retry_policy` varchar(512) DEFAULT NULL COMMENT '通知重试策略',
  `demo_app` tinyint NOT NULL DEFAULT '0' COMMENT '是否示例应用',
  `ip_whitelist_enabled` tinyint NOT NULL DEFAULT '0' COMMENT 'IP 白名单开关',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_application_tenant_app_id` (`tenant_id`,`app_id`,`del_flag`),
  KEY `idx_payment_application_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付接入应用';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_business_order` (
  `id` bigint NOT NULL COMMENT '主键',
  `biz_order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '支付标题',
  `subject_id` bigint NOT NULL COMMENT '收款主体ID',
  `amount` bigint NOT NULL COMMENT '订单金额，单位分',
  `paid_amount` bigint NOT NULL DEFAULT '0' COMMENT '已支付金额，单位分',
  `refunded_amount` bigint NOT NULL DEFAULT '0' COMMENT '已退款金额，单位分',
  `currency` varchar(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `status` varchar(32) NOT NULL COMMENT '业务订单状态',
  `expire_time` datetime DEFAULT NULL COMMENT '订单过期时间',
  `notify_url` varchar(512) DEFAULT NULL COMMENT '本订单支付事件通知地址',
  `return_url` varchar(512) DEFAULT NULL COMMENT '本订单支付完成返回地址',
  `extend_info` json DEFAULT NULL COMMENT '业务扩展信息',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_business_order_tenant_app_no` (`tenant_id`,`app_code`,`biz_order_no`),
  KEY `idx_payment_business_order_tenant_status` (`tenant_id`,`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付业务订单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_cashier_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `cashier_name` varchar(128) NOT NULL COMMENT '收银台名称',
  `application_id` bigint NOT NULL COMMENT '适用应用ID',
  `result_return_url` varchar(512) DEFAULT NULL COMMENT '结果跳转地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `method_codes` varchar(1024) DEFAULT NULL COMMENT '可见标准支付方式编码',
  `default_method_code` varchar(64) DEFAULT NULL COMMENT '默认标准支付方式编码',
  `method_display_order` varchar(1024) DEFAULT NULL COMMENT '支付方式展示顺序',
  `display_config` text COMMENT '基础展示主体配置 JSON',
  `default_cashier` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认收银台',
  `enterprise_subject_ids` varchar(1024) DEFAULT NULL COMMENT '允许企业主体 ID，逗号分隔',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  KEY `idx_payment_cashier_tenant_status` (`tenant_id`,`status`),
  KEY `idx_payment_cashier_tenant_app` (`tenant_id`,`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收银台配置';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel` (
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
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `channel_type` varchar(32) NOT NULL DEFAULT 'AGGREGATOR' COMMENT '通道类型',
  `adapter_type` varchar(64) NOT NULL DEFAULT 'UNCONFIGURED' COMMENT '适配器类型',
  `gateway_base_url` varchar(512) DEFAULT NULL COMMENT '基础网关地址',
  `field_template_json` text COMMENT '签约字段模板 JSON',
  `capability_summary` varchar(1024) DEFAULT NULL COMMENT '通道能力摘要',
  `bill_fetch_modes` varchar(128) DEFAULT NULL COMMENT '支持的账单获取方式：MANUAL/FTP/FTPS/HTTP',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_tenant_code_env_merchant` (`tenant_id`,`channel_code`,`environment`,`merchant_no`,`del_flag`),
  KEY `idx_payment_channel_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_bill_batch` (
  `id` bigint NOT NULL COMMENT '主键',
  `batch_no` varchar(64) NOT NULL COMMENT '账单批次号',
  `reconciliation_id` bigint DEFAULT NULL COMMENT '对账批次ID',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `bill_date` date NOT NULL COMMENT '账单日期',
  `file_digest` varchar(128) NOT NULL COMMENT '账单文件摘要',
  `bill_file_id` bigint DEFAULT NULL COMMENT '账单文件ID',
  `bill_file_name` varchar(255) DEFAULT NULL COMMENT '账单文件名',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '账单笔数',
  `total_amount` bigint NOT NULL DEFAULT '0' COMMENT '账单金额，单位分',
  `total_fee` bigint NOT NULL DEFAULT '0' COMMENT '通道手续费，单位分',
  `import_status` varchar(32) NOT NULL DEFAULT 'IMPORTED' COMMENT '导入状态',
  `importer_id` bigint DEFAULT NULL COMMENT '导入人ID',
  `importer_name` varchar(128) DEFAULT NULL COMMENT '导入人名称',
  `import_time` datetime DEFAULT NULL COMMENT '导入时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_bill_batch_file` (`tenant_id`,`channel_code`,`bill_date`,`file_digest`,`del_flag`),
  UNIQUE KEY `uk_payment_channel_bill_batch_no` (`tenant_id`,`batch_no`,`del_flag`),
  KEY `idx_payment_channel_bill_batch_status` (`tenant_id`,`channel_code`,`bill_date`,`import_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道账单批次';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_bill_detail` (
  `id` bigint NOT NULL COMMENT '主键',
  `reconciliation_id` bigint NOT NULL COMMENT '对账批次ID',
  `batch_no` varchar(64) NOT NULL COMMENT '账单批次号',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `bill_date` date NOT NULL COMMENT '账单日期',
  `channel_trade_no` varchar(128) NOT NULL COMMENT '通道交易号',
  `trade_type` varchar(32) NOT NULL COMMENT '交易类型：PAYMENT、REFUND、FEE',
  `amount` bigint NOT NULL DEFAULT '0' COMMENT '金额，单位分',
  `fee` bigint NOT NULL DEFAULT '0' COMMENT '手续费，单位分',
  `trade_time` datetime NOT NULL COMMENT '通道交易时间',
  `match_status` varchar(32) NOT NULL COMMENT '匹配状态',
  `matched_order_no` varchar(64) DEFAULT NULL COMMENT '匹配到的订单号',
  `match_message` varchar(512) DEFAULT NULL COMMENT '匹配说明',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_bill_detail_trade` (`tenant_id`,`reconciliation_id`,`channel_trade_no`,`trade_type`),
  KEY `idx_payment_bill_detail_tenant_bill` (`tenant_id`,`channel_code`,`bill_date`),
  KEY `idx_payment_bill_detail_match_status` (`tenant_id`,`match_status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道账单明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_bill_fetch_batch` (
  `id` bigint NOT NULL COMMENT '主键',
  `source_id` bigint NOT NULL COMMENT '账单获取源ID',
  `batch_no` varchar(64) NOT NULL COMMENT '获取批次号',
  `reconciliation_id` bigint DEFAULT NULL COMMENT '对账批次ID',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `fetch_mode` varchar(16) NOT NULL COMMENT '获取方式',
  `bill_date` date NOT NULL COMMENT '账单日期',
  `request_start_time` datetime DEFAULT NULL COMMENT '请求开始时间',
  `request_end_time` datetime DEFAULT NULL COMMENT '请求结束时间',
  `request_cursor` varchar(128) DEFAULT NULL COMMENT '请求游标',
  `request_page` int DEFAULT NULL COMMENT '请求页码',
  `page_size` int DEFAULT NULL COMMENT '每页数量',
  `response_digest` varchar(128) DEFAULT NULL COMMENT '响应摘要',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '获取明细数',
  `fetch_status` varchar(32) NOT NULL COMMENT '获取状态：SUCCESS/FAILED',
  `fetch_result` varchar(512) DEFAULT NULL COMMENT '获取结果',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人名称',
  `fetch_start_time` datetime NOT NULL COMMENT '获取开始时间',
  `fetch_end_time` datetime DEFAULT NULL COMMENT '获取结束时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_bill_fetch_batch_no` (`tenant_id`,`batch_no`,`del_flag`),
  KEY `idx_payment_bill_fetch_source` (`tenant_id`,`source_id`,`bill_date`,`fetch_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道账单获取批次';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_bill_source` (
  `id` bigint NOT NULL COMMENT '主键',
  `contract_id` bigint NOT NULL COMMENT '签约通道ID',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `fetch_mode` varchar(16) NOT NULL COMMENT '获取方式：MANUAL/FTP/FTPS/HTTP',
  `endpoint` varchar(255) DEFAULT NULL COMMENT 'HTTP地址或FTP/FTPS服务器地址',
  `remote_path` varchar(255) DEFAULT NULL COMMENT 'FTP/FTPS远端路径',
  `credential_ref` varchar(255) DEFAULT NULL COMMENT '认证配置引用',
  `page_mode` varchar(32) DEFAULT NULL COMMENT 'HTTP分页模式：PAGE/CURSOR',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：1-启用，0-停用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_bill_source_contract_mode` (`tenant_id`,`contract_id`,`fetch_mode`,`del_flag`),
  KEY `idx_payment_bill_source_tenant` (`tenant_id`,`channel_code`,`enabled`),
  KEY `idx_payment_bill_source_contract` (`tenant_id`,`contract_id`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道账单获取源配置';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_capability` (
  `id` bigint NOT NULL COMMENT '主键',
  `channel_id` bigint NOT NULL COMMENT '支付通道 ID',
  `method_code` varchar(64) NOT NULL COMMENT '标准支付方式编码',
  `terminal_type` varchar(32) NOT NULL COMMENT '终端类型',
  `environment` varchar(32) NOT NULL COMMENT '环境',
  `supports_refund` tinyint NOT NULL DEFAULT '1' COMMENT '是否支持退款',
  `supports_query` tinyint NOT NULL DEFAULT '1' COMMENT '是否支持查单',
  `supports_close` tinyint NOT NULL DEFAULT '1' COMMENT '是否支持关单',
  `supports_bill` tinyint NOT NULL DEFAULT '1' COMMENT '是否支持账单',
  `supports_reconcile` tinyint NOT NULL DEFAULT '1' COMMENT '是否支持对账',
  `min_amount` bigint DEFAULT NULL COMMENT '最小金额，单位分',
  `max_amount` bigint DEFAULT NULL COMMENT '最大金额，单位分',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_capability` (`tenant_id`,`channel_id`,`method_code`,`terminal_type`,`environment`,`del_flag`),
  KEY `idx_payment_channel_capability_route` (`tenant_id`,`method_code`,`terminal_type`,`environment`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道能力';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_certificate_rotation_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `contract_id` bigint NOT NULL COMMENT '通道签约ID',
  `contract_capability_id` bigint NOT NULL COMMENT '签约能力ID',
  `certificate_field_code` varchar(64) NOT NULL COMMENT '证书文件字段编码',
  `old_certificate_file_id` bigint DEFAULT NULL COMMENT '旧证书文件ID',
  `new_certificate_file_id` bigint NOT NULL COMMENT '新证书文件ID',
  `old_certificate_expire_time` datetime DEFAULT NULL COMMENT '旧证书有效期',
  `new_certificate_expire_time` datetime NOT NULL COMMENT '新证书有效期',
  `rotate_reason` varchar(512) NOT NULL COMMENT '轮换原因',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人名称',
  `rotate_time` datetime NOT NULL COMMENT '轮换时间',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  KEY `idx_payment_channel_cert_rotation_contract` (`tenant_id`,`contract_id`,`rotate_time`),
  KEY `idx_payment_channel_cert_rotation_capability` (`tenant_id`,`contract_capability_id`,`rotate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道签约证书轮换记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_contract` (
  `id` bigint NOT NULL COMMENT '主键',
  `contract_code` varchar(64) NOT NULL COMMENT '签约编码',
  `contract_name` varchar(128) NOT NULL COMMENT '签约名称',
  `subject_id` bigint NOT NULL COMMENT '企业主体 ID',
  `channel_id` bigint NOT NULL COMMENT '支付通道 ID',
  `environment` varchar(32) NOT NULL COMMENT '签约环境',
  `merchant_no` varchar(64) DEFAULT NULL COMMENT '商户号',
  `app_id` varchar(128) DEFAULT NULL COMMENT '通道 AppId',
  `config_values_json` text COMMENT '按通道字段模板填写的配置值 JSON',
  `enabled_method_codes` varchar(1024) DEFAULT NULL COMMENT '已开通标准支付方式编码',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_contract_tenant_code` (`tenant_id`,`contract_code`,`del_flag`),
  KEY `idx_payment_contract_subject` (`tenant_id`,`subject_id`,`status`),
  KEY `idx_payment_contract_channel` (`tenant_id`,`channel_id`,`environment`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道签约配置';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_contract_capability` (
  `id` bigint NOT NULL COMMENT '主键',
  `contract_id` bigint NOT NULL COMMENT '通道签约 ID',
  `channel_capability_id` bigint NOT NULL COMMENT '通道能力 ID',
  `method_code` varchar(64) NOT NULL COMMENT '标准支付方式编码',
  `terminal_type` varchar(32) NOT NULL COMMENT '终端类型',
  `fee_rate` decimal(10,10) DEFAULT NULL COMMENT '费率，最多保留10位小数',
  `min_amount` bigint DEFAULT NULL COMMENT '最小金额，单位分',
  `max_amount` bigint DEFAULT NULL COMMENT '最大金额，单位分',
  `priority` int NOT NULL DEFAULT '100' COMMENT '路由优先级',
  `certificate_expire_time` datetime DEFAULT NULL COMMENT '证书有效期',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_contract_capability` (`tenant_id`,`contract_id`,`channel_capability_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付签约能力';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_contract_value` (
  `id` bigint NOT NULL COMMENT '主键',
  `contract_id` bigint NOT NULL COMMENT '通道签约ID',
  `field_code` varchar(64) NOT NULL COMMENT '字段编码',
  `value_text` varchar(1024) DEFAULT NULL COMMENT '非敏感签约值',
  `encrypted_value` text COMMENT '敏感签约密文值',
  `file_id` bigint DEFAULT NULL COMMENT '文件中心ID',
  `value_source` varchar(32) NOT NULL DEFAULT 'CONFIG' COMMENT '值来源：CONFIG、COLUMN、FILE',
  `sensitive_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否敏感',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_contract_value` (`tenant_id`,`contract_id`,`field_code`,`del_flag`),
  KEY `idx_payment_channel_contract_value_contract` (`tenant_id`,`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道签约配置值';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_field_template` (
  `id` bigint NOT NULL COMMENT '主键',
  `channel_id` bigint NOT NULL COMMENT '支付通道ID',
  `field_code` varchar(64) NOT NULL COMMENT '字段编码',
  `field_label` varchar(128) NOT NULL COMMENT '字段名称',
  `component_type` varchar(64) NOT NULL COMMENT '控件类型',
  `data_type` varchar(32) NOT NULL DEFAULT 'string' COMMENT '数据类型',
  `required_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否必填',
  `sensitive_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否敏感',
  `encrypted_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否加密保存',
  `masked_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否脱敏展示',
  `file_reference_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否文件中心引用',
  `option_json` text COMMENT '选项 JSON',
  `validation_json` text COMMENT '校验规则 JSON',
  `field_group` varchar(64) DEFAULT NULL COMMENT '字段分组',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_field_template` (`tenant_id`,`channel_id`,`field_code`,`del_flag`),
  KEY `idx_payment_channel_field_template_channel` (`tenant_id`,`channel_id`,`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道签约字段模板';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_query_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `query_no` varchar(64) NOT NULL COMMENT '查单记录号',
  `pay_order_no` varchar(64) NOT NULL COMMENT '支付订单号',
  `channel_trade_no` varchar(128) DEFAULT NULL COMMENT '通道交易号',
  `payment_order_id` bigint NOT NULL COMMENT '支付订单ID',
  `business_order_id` bigint NOT NULL COMMENT '业务订单ID',
  `channel_id` bigint DEFAULT NULL COMMENT '支付通道ID',
  `contract_id` bigint DEFAULT NULL COMMENT '签约配置ID',
  `query_type` varchar(32) NOT NULL COMMENT '查单类型',
  `request_payload` varchar(1024) DEFAULT NULL COMMENT '查单请求摘要',
  `response_payload` varchar(2048) DEFAULT NULL COMMENT '查单响应摘要',
  `before_status` varchar(32) NOT NULL COMMENT '查单前状态',
  `channel_status` varchar(32) NOT NULL COMMENT '通道返回状态',
  `result_status` varchar(32) NOT NULL COMMENT '处理后状态',
  `process_result` varchar(64) NOT NULL COMMENT '处理结果',
  `process_message` varchar(512) DEFAULT NULL COMMENT '处理说明',
  `query_time` datetime NOT NULL COMMENT '查单时间',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_query_no` (`query_no`),
  KEY `idx_payment_channel_query_order` (`tenant_id`,`pay_order_no`,`query_time`),
  KEY `idx_payment_channel_query_payment` (`tenant_id`,`payment_order_id`,`query_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道主动查单记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_difference` (
  `id` bigint NOT NULL COMMENT '主键',
  `difference_no` varchar(64) NOT NULL COMMENT '差异单号',
  `reconciliation_id` bigint NOT NULL COMMENT '对账批次ID',
  `related_order_no` varchar(64) NOT NULL COMMENT '关联订单号',
  `difference_type` varchar(64) NOT NULL COMMENT '差异类型',
  `difference_amount` bigint NOT NULL DEFAULT '0' COMMENT '差异金额，单位分',
  `process_status` varchar(32) NOT NULL COMMENT '处理状态',
  `process_action` varchar(64) DEFAULT NULL COMMENT '处理动作',
  `process_reason` varchar(512) DEFAULT NULL COMMENT '处理原因',
  `process_result` varchar(512) DEFAULT NULL COMMENT '处理结果',
  `process_evidence` varchar(512) DEFAULT NULL COMMENT '处理凭据文件ID或业务凭据token',
  `adjust_flow_id` bigint DEFAULT NULL COMMENT '差异处理备注流水ID',
  `adjust_flow_no` varchar(64) DEFAULT NULL COMMENT '差异处理备注流水号',
  `processor_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `processor_name` varchar(128) DEFAULT NULL COMMENT '处理人名称',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_difference_no` (`difference_no`),
  KEY `idx_payment_difference_tenant_status` (`tenant_id`,`process_status`,`created_at`),
  KEY `idx_payment_difference_reconciliation` (`tenant_id`,`reconciliation_id`,`difference_type`),
  KEY `idx_payment_difference_tenant_type_status` (`tenant_id`,`difference_type`,`process_status`),
  KEY `idx_payment_difference_adjust_flow` (`tenant_id`,`adjust_flow_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付对账差异';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_enterprise_subject` (
  `id` bigint NOT NULL COMMENT '主键',
  `subject_name` varchar(128) NOT NULL COMMENT '主体名称',
  `credit_code` varchar(512) NOT NULL COMMENT '统一社会信用代码密文',
  `credit_code_hash` varchar(64) NOT NULL COMMENT '统一社会信用代码规范化哈希',
  `bank_account_no` varchar(512) NOT NULL COMMENT '银行账户密文',
  `bank_name` varchar(128) NOT NULL COMMENT '开户行',
  `license_file_id` bigint DEFAULT NULL COMMENT '证照文件ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_subject_tenant_credit_hash` (`tenant_id`,`credit_code_hash`,`del_flag`),
  KEY `idx_payment_subject_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付收款主体';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_exception_order` (
  `id` bigint NOT NULL COMMENT '主键',
  `exception_no` varchar(64) NOT NULL COMMENT '异常单号',
  `related_order_no` varchar(64) NOT NULL COMMENT '关联订单号',
  `exception_type` varchar(64) NOT NULL COMMENT '异常类型',
  `active_business_key` varchar(256) GENERATED ALWAYS AS ((case when (`del_flag` = 0) then concat(`tenant_id`,_utf8mb4'|',`related_order_no`,_utf8mb4'|',`exception_type`) else NULL end)) STORED COMMENT '未删除异常订单业务幂等键',
  `severity` varchar(32) NOT NULL COMMENT '异常级别',
  `handle_status` varchar(32) NOT NULL COMMENT '处理状态',
  `reason` varchar(512) DEFAULT NULL COMMENT '异常原因',
  `handle_action` varchar(64) DEFAULT NULL COMMENT '处理动作',
  `handle_reason` varchar(512) DEFAULT NULL COMMENT '处理原因',
  `handle_result` varchar(512) DEFAULT NULL COMMENT '处理结果',
  `handle_evidence` varchar(512) DEFAULT NULL COMMENT '处理凭据',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(128) DEFAULT NULL COMMENT '处理人名称',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_exception_no` (`exception_no`),
  UNIQUE KEY `uk_payment_exception_active_business` (`active_business_key`),
  KEY `idx_payment_exception_tenant_status` (`tenant_id`,`handle_status`,`created_at`),
  KEY `idx_payment_exception_tenant_type_status_time` (`tenant_id`,`exception_type`,`handle_status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付异常订单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_mango_pay_scenario_control` (
  `id` bigint NOT NULL COMMENT '主键',
  `control_no` varchar(64) NOT NULL COMMENT '场景控制编号',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码，仅支持 MANGO_PAY',
  `contract_id` bigint DEFAULT NULL COMMENT '签约配置ID；为空表示芒果支付全局控制',
  `scenario_type` varchar(32) NOT NULL COMMENT '场景类型：PAYMENT/PAYMENT_QUERY/REFUND/REFUND_QUERY/BILL/CALLBACK_DELAY',
  `scenario_code` varchar(64) DEFAULT NULL COMMENT '交易场景码',
  `bill_difference_type` varchar(32) DEFAULT NULL COMMENT '账单差异类型：AMOUNT_PLUS/AMOUNT_MINUS',
  `difference_amount` bigint DEFAULT NULL COMMENT '账单差异金额，单位分',
  `callback_delay_minutes` int DEFAULT NULL COMMENT '回调延迟分钟数',
  `effective_count` int NOT NULL DEFAULT '1' COMMENT '生效次数',
  `consumed_count` int NOT NULL DEFAULT '0' COMMENT '已消费次数',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/CONSUMED/DISABLED',
  `consumed_at` datetime DEFAULT NULL COMMENT '最近消费时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_mango_pay_scenario_control_no` (`tenant_id`,`control_no`),
  KEY `idx_payment_mango_pay_scenario_next` (`tenant_id`,`channel_code`,`scenario_type`,`contract_id`,`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='芒果支付异常场景控制';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_method` (
  `id` bigint NOT NULL COMMENT '主键',
  `method_code` varchar(64) NOT NULL COMMENT '支付方式编码',
  `method_name` varchar(128) NOT NULL COMMENT '支付方式名称',
  `channel_id` bigint DEFAULT NULL COMMENT '历史字段：支付方式不得直接绑定通道',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `account_nature` varchar(32) NOT NULL DEFAULT 'PERSONAL' COMMENT '一级分类：对公/对私',
  `instrument_type` varchar(32) NOT NULL DEFAULT 'WECHAT' COMMENT '二级分类：网银/线下/支付宝/微信/银联等',
  `interaction_type` varchar(32) NOT NULL DEFAULT 'QR_CODE' COMMENT '三级分类：扫码/H5/网银跳转/账号转账/快捷等',
  `terminal_scope` varchar(64) NOT NULL DEFAULT 'WEB,H5' COMMENT '终端范围',
  `payment_material_type` varchar(32) NOT NULL DEFAULT 'QR' COMMENT '支付物料类型',
  `cashier_group_code` varchar(64) NOT NULL COMMENT '收银台展示分组编码',
  `cashier_group_name` varchar(128) NOT NULL COMMENT '收银台展示分组名称',
  `cashier_group_sort` int NOT NULL DEFAULT '0' COMMENT '收银台展示分组排序',
  `icon_file_id` bigint DEFAULT NULL COMMENT '图标文件 ID',
  `requires_bank_selection` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要银行列表：0-否，1-是',
  `requires_qr_refresh` tinyint NOT NULL DEFAULT '0' COMMENT '二维码是否支持刷新：0-否，1-是',
  `description` varchar(512) DEFAULT NULL COMMENT '收银台说明',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_method_tenant_code` (`tenant_id`,`method_code`,`del_flag`),
  KEY `idx_payment_method_tenant_status` (`tenant_id`,`status`),
  KEY `idx_payment_method_cashier_group` (`tenant_id`,`cashier_group_sort`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_method_category` (
  `id` bigint NOT NULL COMMENT '主键',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `category_name` varchar(128) NOT NULL COMMENT '分类名称',
  `level` tinyint NOT NULL COMMENT '层级：1-账户属性，2-支付工具/网络，3-交互/产品形态',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级分类 ID，根节点为 0',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_method_category` (`tenant_id`,`level`,`parent_id`,`category_code`,`del_flag`),
  KEY `idx_payment_method_category_parent` (`tenant_id`,`parent_id`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式三级分类字典';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_method_route_rule` (
  `id` bigint NOT NULL COMMENT '主键',
  `rule_code` varchar(64) NOT NULL COMMENT '路由规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '路由规则名称',
  `app_id` bigint DEFAULT NULL COMMENT '应用 ID',
  `subject_id` bigint DEFAULT NULL COMMENT '企业主体 ID',
  `method_code` varchar(64) NOT NULL COMMENT '标准支付方式编码',
  `terminal_type` varchar(32) NOT NULL COMMENT '终端类型',
  `environment` varchar(32) NOT NULL COMMENT '环境',
  `route_mode` varchar(32) NOT NULL COMMENT '路由模式',
  `fallback_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许失败降级',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_route_rule` (`tenant_id`,`rule_code`,`del_flag`),
  KEY `idx_payment_route_rule_match` (`tenant_id`,`app_id`,`subject_id`,`method_code`,`terminal_type`,`environment`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式路由规则';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_method_route_rule_item` (
  `id` bigint NOT NULL COMMENT '主键',
  `rule_id` bigint NOT NULL COMMENT '路由规则 ID',
  `contract_capability_id` bigint NOT NULL COMMENT '签约能力 ID',
  `priority` int NOT NULL DEFAULT '100' COMMENT '优先级',
  `weight` int NOT NULL DEFAULT '100' COMMENT '权重',
  `min_amount` bigint DEFAULT NULL COMMENT '最小金额，单位分',
  `max_amount` bigint DEFAULT NULL COMMENT '最大金额，单位分',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_route_rule_item` (`tenant_id`,`rule_id`,`contract_capability_id`,`del_flag`),
  KEY `idx_payment_route_item_rule` (`tenant_id`,`rule_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式路由规则明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_notification_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `notification_no` varchar(64) NOT NULL COMMENT '通知单号',
  `related_order_no` varchar(64) NOT NULL COMMENT '关联订单号',
  `notification_type` varchar(64) NOT NULL COMMENT '通知类型',
  `target_url` varchar(512) NOT NULL COMMENT '通知目标地址',
  `notify_status` varchar(32) NOT NULL COMMENT '通知状态',
  `retry_times` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `scheduled_notify_time` datetime DEFAULT NULL COMMENT '计划通知时间',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下一次重试时间',
  `payload_json` text COMMENT '通知报文快照',
  `response_code` varchar(64) DEFAULT NULL COMMENT '响应码',
  `response_message` varchar(512) DEFAULT NULL COMMENT '响应信息',
  `last_manual_retry_time` datetime DEFAULT NULL COMMENT '最后人工重推时间',
  `last_manual_retry_reason` varchar(512) DEFAULT NULL COMMENT '最后人工重推原因',
  `last_manual_retry_result` varchar(512) DEFAULT NULL COMMENT '最后人工重推结果',
  `last_manual_retry_operator_id` bigint DEFAULT NULL COMMENT '最后人工重推人ID',
  `last_manual_retry_operator_name` varchar(128) DEFAULT NULL COMMENT '最后人工重推人名称',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_notification_no` (`notification_no`),
  KEY `idx_payment_notification_tenant_status` (`tenant_id`,`notify_status`,`created_at`),
  KEY `idx_payment_notification_tenant_type_status_time` (`tenant_id`,`notification_type`,`notify_status`,`created_at`),
  KEY `idx_payment_notification_schedule` (`tenant_id`,`notify_status`,`scheduled_notify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通知记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_offline_bank_statement_batch` (
  `id` bigint NOT NULL COMMENT '主键',
  `batch_no` varchar(64) NOT NULL COMMENT '线下银行流水导入批次号',
  `bank_account_no_mask` varchar(128) DEFAULT NULL COMMENT '脱敏收款账号',
  `bank_name` varchar(128) DEFAULT NULL COMMENT '收款开户行',
  `statement_file_id` bigint DEFAULT NULL COMMENT '银行流水文件ID',
  `statement_file_name` varchar(255) NOT NULL COMMENT '银行流水文件名',
  `file_digest` varchar(128) NOT NULL COMMENT '银行流水文件摘要',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '导入总笔数',
  `matched_count` int NOT NULL DEFAULT '0' COMMENT '匹配笔数',
  `confirmed_count` int NOT NULL DEFAULT '0' COMMENT '确认笔数',
  `difference_count` int NOT NULL DEFAULT '0' COMMENT '差异笔数',
  `batch_status` varchar(32) NOT NULL COMMENT '批次状态',
  `importer_id` bigint DEFAULT NULL COMMENT '导入人ID',
  `importer_name` varchar(128) DEFAULT NULL COMMENT '导入人名称',
  `import_time` datetime NOT NULL COMMENT '导入时间',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_offline_bank_statement_batch_no` (`tenant_id`,`batch_no`,`del_flag`),
  UNIQUE KEY `uk_payment_offline_bank_statement_file` (`tenant_id`,`file_digest`,`del_flag`),
  KEY `idx_payment_offline_bank_statement_status_time` (`tenant_id`,`batch_status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线下收款银行流水导入批次';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_offline_bank_statement_item` (
  `id` bigint NOT NULL COMMENT '主键',
  `batch_id` bigint NOT NULL COMMENT '导入批次ID',
  `batch_no` varchar(64) NOT NULL COMMENT '导入批次号',
  `row_no` int NOT NULL COMMENT 'Excel行号',
  `bank_statement_no` varchar(128) NOT NULL COMMENT '银行流水号',
  `bank_account_no_mask` varchar(128) DEFAULT NULL COMMENT '脱敏收款账号',
  `bank_name` varchar(128) DEFAULT NULL COMMENT '收款开户行',
  `trade_time` datetime NOT NULL COMMENT '银行交易时间',
  `trade_date` date NOT NULL COMMENT '银行交易日期',
  `amount` bigint NOT NULL COMMENT '收入金额，单位分',
  `currency` varchar(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `counterparty_name` varchar(128) DEFAULT NULL COMMENT '对方户名',
  `counterparty_account_no_mask` varchar(128) DEFAULT NULL COMMENT '脱敏对方账号',
  `summary` varchar(512) DEFAULT NULL COMMENT '银行摘要',
  `remark` varchar(512) DEFAULT NULL COMMENT '银行备注',
  `reconciliation_code` varchar(32) DEFAULT NULL COMMENT '解析出的转账备注识别码',
  `matched_offline_collection_id` bigint DEFAULT NULL COMMENT '匹配线下收款ID',
  `matched_offline_collection_no` varchar(64) DEFAULT NULL COMMENT '匹配线下收款单号',
  `matched_pay_order_no` varchar(64) DEFAULT NULL COMMENT '匹配支付订单号',
  `match_status` varchar(32) NOT NULL COMMENT '匹配状态',
  `match_message` varchar(512) NOT NULL COMMENT '匹配说明',
  `confirmed_time` datetime DEFAULT NULL COMMENT '确认时间',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人ID',
  `confirmed_by_name` varchar(128) DEFAULT NULL COMMENT '确认人名称',
  `confirm_remark` varchar(512) DEFAULT NULL COMMENT '确认说明',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_offline_bank_statement_item_no` (`tenant_id`,`bank_account_no_mask`,`bank_statement_no`,`trade_date`,`del_flag`),
  KEY `idx_payment_offline_bank_statement_item_batch` (`tenant_id`,`batch_id`,`row_no`),
  KEY `idx_payment_offline_bank_statement_item_match` (`tenant_id`,`match_status`,`created_at`),
  KEY `idx_payment_offline_bank_statement_item_collection` (`tenant_id`,`matched_offline_collection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线下收款银行流水明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_offline_collection` (
  `id` bigint NOT NULL COMMENT '主键',
  `offline_collection_no` varchar(64) NOT NULL COMMENT '线下收款单号',
  `payment_order_id` bigint NOT NULL COMMENT '支付订单ID',
  `pay_order_no` varchar(64) NOT NULL COMMENT '支付订单号',
  `business_order_id` bigint NOT NULL COMMENT '业务订单ID',
  `biz_order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `channel_id` bigint DEFAULT NULL COMMENT '支付通道ID',
  `channel_code` varchar(32) NOT NULL COMMENT '支付通道编码',
  `contract_id` bigint DEFAULT NULL COMMENT '通道签约配置ID',
  `contract_capability_id` bigint DEFAULT NULL COMMENT '签约能力ID',
  `subject_id` bigint NOT NULL COMMENT '企业主体ID',
  `subject_name` varchar(128) NOT NULL COMMENT '企业主体名称',
  `bank_account_id` bigint DEFAULT NULL COMMENT '收款银行账户ID',
  `account_name` varchar(128) NOT NULL COMMENT '收款户名',
  `account_no_mask` varchar(128) NOT NULL COMMENT '脱敏收款账号',
  `bank_name` varchar(128) NOT NULL COMMENT '开户行',
  `amount` bigint NOT NULL COMMENT '收款金额，单位分',
  `currency` varchar(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `transfer_amount` bigint DEFAULT NULL COMMENT '用户提交转账金额，单位分',
  `voucher_file_ids` varchar(512) DEFAULT NULL COMMENT '转账凭证文件ID，多个用英文逗号分隔',
  `submitted_time` datetime DEFAULT NULL COMMENT '用户提交凭证时间',
  `submit_remark` varchar(512) DEFAULT NULL COMMENT '用户提交说明',
  `confirmed_amount` bigint DEFAULT NULL COMMENT '确认到账金额，单位分',
  `reconciliation_code` varchar(32) NOT NULL COMMENT '转账备注识别码',
  `transfer_remark` varchar(128) NOT NULL COMMENT '用户转账备注',
  `voucher_count` int NOT NULL DEFAULT '0' COMMENT '上传凭证数量',
  `collection_status` varchar(32) NOT NULL DEFAULT 'WAITING_TRANSFER' COMMENT '线下收款状态',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `confirmed_time` datetime DEFAULT NULL COMMENT '确认到账时间',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人ID',
  `confirmed_by_name` varchar(128) DEFAULT NULL COMMENT '确认人名称',
  `confirm_remark` varchar(512) DEFAULT NULL COMMENT '确认说明',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_offline_collection_no` (`tenant_id`,`offline_collection_no`,`del_flag`),
  UNIQUE KEY `uk_payment_offline_reconciliation_code` (`tenant_id`,`reconciliation_code`,`del_flag`),
  KEY `idx_payment_offline_collection_status_time` (`tenant_id`,`collection_status`,`created_at`),
  KEY `idx_payment_offline_collection_pay_order` (`tenant_id`,`pay_order_no`),
  KEY `idx_payment_offline_collection_biz_order` (`tenant_id`,`biz_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线下收款单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_offline_collection_match` (
  `id` bigint NOT NULL COMMENT '主键',
  `offline_collection_id` bigint NOT NULL COMMENT '线下收款ID',
  `offline_collection_no` varchar(64) NOT NULL COMMENT '线下收款单号',
  `bank_statement_item_id` bigint NOT NULL COMMENT '银行流水明细ID',
  `bank_statement_no` varchar(128) NOT NULL COMMENT '银行流水号',
  `pay_order_no` varchar(64) NOT NULL COMMENT '支付订单号',
  `match_rule` varchar(64) NOT NULL COMMENT '匹配规则',
  `match_status` varchar(32) NOT NULL COMMENT '匹配状态',
  `difference_type` varchar(64) DEFAULT NULL COMMENT '差异类型',
  `match_message` varchar(512) NOT NULL COMMENT '匹配说明',
  `confirmed_time` datetime DEFAULT NULL COMMENT '确认时间',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人ID',
  `confirmed_by_name` varchar(128) DEFAULT NULL COMMENT '确认人名称',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_offline_collection_match_item` (`tenant_id`,`bank_statement_item_id`,`del_flag`),
  KEY `idx_payment_offline_collection_match_collection` (`tenant_id`,`offline_collection_id`,`match_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线下收款银行流水匹配结果';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_offline_collection_voucher` (
  `id` bigint NOT NULL COMMENT '主键',
  `offline_collection_id` bigint NOT NULL COMMENT '线下收款ID',
  `offline_collection_no` varchar(64) NOT NULL COMMENT '线下收款单号',
  `pay_order_no` varchar(64) NOT NULL COMMENT '支付订单号',
  `voucher_file_id` varchar(64) NOT NULL COMMENT '支付凭证文件ID',
  `upload_source` varchar(32) NOT NULL COMMENT '上传来源：CASHIER/ADMIN/BANK_IMPORT',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传人ID',
  `uploader_name` varchar(128) DEFAULT NULL COMMENT '上传人名称',
  `upload_time` datetime NOT NULL COMMENT '上传时间',
  `review_status` varchar(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态：SUBMITTED/ACCEPTED/REJECTED',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_offline_collection_voucher_file` (`tenant_id`,`offline_collection_id`,`voucher_file_id`,`del_flag`),
  KEY `idx_payment_offline_collection_voucher_no` (`tenant_id`,`offline_collection_no`),
  KEY `idx_payment_offline_collection_voucher_pay` (`tenant_id`,`pay_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线下收款支付凭证';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_offline_refund_process` (
  `id` bigint NOT NULL COMMENT '主键',
  `offline_refund_no` varchar(64) NOT NULL COMMENT '线下退款单号',
  `offline_collection_id` bigint NOT NULL COMMENT '线下收款ID',
  `offline_collection_no` varchar(64) NOT NULL COMMENT '线下收款单号',
  `refund_order_id` bigint DEFAULT NULL COMMENT '统一退款订单ID',
  `payment_order_id` bigint NOT NULL COMMENT '原支付订单ID',
  `pay_order_no` varchar(64) NOT NULL COMMENT '原支付订单号',
  `business_order_id` bigint NOT NULL COMMENT '业务订单ID',
  `biz_order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `channel_id` bigint DEFAULT NULL COMMENT '支付通道ID',
  `channel_code` varchar(32) NOT NULL COMMENT '支付通道编码',
  `refund_amount` bigint NOT NULL COMMENT '退款金额，单位分',
  `currency` varchar(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `refund_account_name` varchar(128) NOT NULL COMMENT '退款账户户名',
  `refund_account_no_mask` varchar(128) NOT NULL COMMENT '脱敏退款账号',
  `refund_bank_name` varchar(128) NOT NULL COMMENT '退款开户行',
  `refund_voucher_file_ids` varchar(512) NOT NULL COMMENT '退款凭证文件ID，多个用英文逗号分隔',
  `refund_voucher_count` int NOT NULL DEFAULT '0' COMMENT '退款凭证数量',
  `reason` varchar(512) NOT NULL COMMENT '退款原因',
  `remark` varchar(512) DEFAULT NULL COMMENT '退款备注',
  `refund_status` varchar(32) NOT NULL DEFAULT 'REFUNDED' COMMENT '线下退款状态',
  `refunded_time` datetime NOT NULL COMMENT '退款完成时间',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人名称',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_offline_refund_process_no` (`tenant_id`,`offline_refund_no`,`del_flag`),
  KEY `idx_payment_offline_refund_process_collection` (`tenant_id`,`offline_collection_id`,`refund_status`),
  KEY `idx_payment_offline_refund_process_payment` (`tenant_id`,`payment_order_id`,`refund_status`),
  KEY `idx_payment_offline_refund_process_business` (`tenant_id`,`business_order_id`,`refund_status`),
  KEY `idx_payment_offline_refund_process_status_time` (`tenant_id`,`refund_status`,`created_at`),
  KEY `idx_payment_offline_refund_process_refund_order` (`tenant_id`,`refund_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线下退款处理记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_openapi_nonce` (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` varchar(64) NOT NULL COMMENT '支付应用 AppId',
  `nonce` varchar(128) NOT NULL COMMENT '开放接口随机串',
  `expire_time` datetime NOT NULL COMMENT 'nonce 过期时间',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_openapi_nonce` (`tenant_id`,`app_id`,`nonce`,`del_flag`),
  KEY `idx_payment_openapi_nonce_expire` (`tenant_id`,`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付开放接口 nonce 防重放';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_operation_audit` (
  `id` bigint NOT NULL COMMENT '主键',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人名称',
  `operation_action` varchar(64) NOT NULL COMMENT '操作动作',
  `resource_type` varchar(64) NOT NULL COMMENT '资源类型',
  `resource_id` varchar(64) DEFAULT NULL COMMENT '资源ID',
  `operation_result` varchar(32) NOT NULL COMMENT '操作结果',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  KEY `idx_payment_audit_tenant_time` (`tenant_id`,`operation_time`),
  KEY `idx_payment_audit_resource` (`resource_type`,`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付操作审计';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_order` (
  `id` bigint NOT NULL COMMENT '主键',
  `pay_order_no` varchar(64) NOT NULL COMMENT '支付订单号',
  `business_order_id` bigint NOT NULL COMMENT '业务订单ID',
  `cashier_config_id` bigint DEFAULT NULL COMMENT '收银台配置ID',
  `channel_id` bigint NOT NULL COMMENT '通道ID',
  `channel_code` varchar(64) NOT NULL COMMENT '支付通道编码',
  `channel_merchant_no` varchar(64) DEFAULT NULL COMMENT '通道商户号',
  `contract_id` bigint DEFAULT NULL COMMENT '通道签约配置 ID',
  `contract_capability_id` bigint DEFAULT NULL COMMENT '签约能力 ID',
  `route_rule_id` bigint DEFAULT NULL COMMENT '路由规则 ID',
  `method_id` bigint NOT NULL COMMENT '支付方式ID',
  `amount` bigint NOT NULL COMMENT '支付金额，单位分',
  `status` varchar(32) NOT NULL COMMENT '支付订单状态',
  `channel_trade_no` varchar(128) DEFAULT NULL COMMENT '通道交易号',
  `payment_material_json` json DEFAULT NULL COMMENT '通道支付物料快照',
  `success_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否为业务订单有效成功支付：1-是，0-否',
  `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `expire_time` datetime DEFAULT NULL COMMENT '支付订单过期时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `success_business_order_id` bigint GENERATED ALWAYS AS ((case when (`success_flag` = 1) then `business_order_id` else NULL end)) STORED COMMENT '有效成功支付唯一约束辅助列',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_order_no` (`pay_order_no`),
  UNIQUE KEY `uk_payment_order_channel_trade` (`tenant_id`,`channel_code`,`channel_trade_no`),
  UNIQUE KEY `uk_payment_order_success_business` (`tenant_id`,`success_business_order_id`),
  KEY `idx_payment_order_business` (`business_order_id`),
  KEY `idx_payment_order_tenant_status` (`tenant_id`,`status`,`created_at`),
  KEY `idx_payment_order_cashier` (`tenant_id`,`cashier_config_id`,`created_at`),
  KEY `idx_payment_order_contract` (`tenant_id`,`contract_id`,`contract_capability_id`),
  KEY `idx_payment_order_success` (`tenant_id`,`business_order_id`,`success_flag`,`status`),
  KEY `idx_payment_order_tenant_status_audit_time` (`tenant_id`,`status`,`created_at`),
  KEY `idx_payment_order_cashier_audit_time` (`tenant_id`,`cashier_config_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_order_status_flow` (
  `id` bigint NOT NULL COMMENT '主键',
  `order_type` varchar(32) NOT NULL COMMENT '订单类型：BUSINESS_ORDER、PAYMENT_ORDER、REFUND_ORDER',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `from_status` varchar(32) DEFAULT NULL COMMENT '变更前状态',
  `to_status` varchar(32) NOT NULL COMMENT '变更后状态',
  `trigger_source` varchar(64) NOT NULL COMMENT '触发来源',
  `trigger_no` varchar(128) DEFAULT NULL COMMENT '触发单号',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人名称',
  `happen_time` datetime NOT NULL COMMENT '发生时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '说明',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  KEY `idx_payment_order_status_flow_order` (`tenant_id`,`order_type`,`order_id`,`happen_time`),
  KEY `idx_payment_order_status_flow_no` (`tenant_id`,`order_type`,`order_no`,`happen_time`),
  KEY `idx_payment_order_status_flow_trigger` (`tenant_id`,`trigger_source`,`trigger_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单状态流转记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_reconciliation` (
  `id` bigint NOT NULL COMMENT '主键',
  `reconciliation_no` varchar(64) NOT NULL COMMENT '对账批次号',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `bill_date` date NOT NULL COMMENT '账单日期',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '账单笔数',
  `total_amount` bigint NOT NULL DEFAULT '0' COMMENT '账单金额，单位分',
  `total_fee` bigint NOT NULL DEFAULT '0' COMMENT '通道手续费，单位分',
  `match_status` varchar(32) NOT NULL COMMENT '匹配状态',
  `bill_file_id` bigint DEFAULT NULL COMMENT '账单文件ID',
  `bill_file_name` varchar(255) NOT NULL COMMENT '账单文件名',
  `file_digest` varchar(128) NOT NULL COMMENT '账单文件摘要',
  `importer_id` bigint DEFAULT NULL COMMENT '导入人ID',
  `importer_name` varchar(128) DEFAULT NULL COMMENT '导入人名称',
  `import_time` datetime DEFAULT NULL COMMENT '导入时间',
  `reconcile_result` varchar(512) DEFAULT NULL COMMENT '对账结果说明',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_reconciliation_no` (`reconciliation_no`),
  UNIQUE KEY `uk_payment_reconciliation_file_digest` (`tenant_id`,`channel_code`,`bill_date`,`file_digest`),
  KEY `idx_payment_reconciliation_tenant_bill` (`tenant_id`,`channel_code`,`bill_date`),
  KEY `idx_payment_reconciliation_tenant_status_time` (`tenant_id`,`match_status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付对账批次';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_refund_approval` (
  `id` bigint NOT NULL COMMENT '主键',
  `approval_no` varchar(64) NOT NULL COMMENT '退款审批单号',
  `business_order_id` bigint NOT NULL COMMENT '业务订单ID',
  `payment_order_id` bigint NOT NULL COMMENT '原支付订单ID',
  `refund_order_id` bigint DEFAULT NULL COMMENT '审批通过后生成的退款订单ID',
  `biz_order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `biz_refund_no` varchar(64) NOT NULL COMMENT '业务退款单号',
  `app_id` varchar(64) NOT NULL COMMENT '支付应用AppId',
  `refund_amount` bigint NOT NULL COMMENT '退款金额，单位分',
  `reason` varchar(512) NOT NULL COMMENT '退款原因',
  `remark` varchar(512) DEFAULT NULL COMMENT '退款备注',
  `status` varchar(32) NOT NULL COMMENT '审批状态：PENDING、APPROVED、REJECTED',
  `workflow_apply_id` bigint DEFAULT NULL COMMENT '工作流申请ID',
  `workflow_process_instance_id` varchar(128) DEFAULT NULL COMMENT '工作流流程实例ID',
  `workflow_process_definition_key` varchar(128) DEFAULT NULL COMMENT '工作流流程定义编码',
  `workflow_apply_status` varchar(64) DEFAULT NULL COMMENT '工作流申请状态',
  `workflow_apply_status_name` varchar(128) DEFAULT NULL COMMENT '工作流申请状态名称',
  `workflow_current_task_names` varchar(512) DEFAULT NULL COMMENT '工作流当前节点名称',
  `workflow_current_assignee_names` varchar(512) DEFAULT NULL COMMENT '工作流当前处理人名称',
  `workflow_synced_at` datetime DEFAULT NULL COMMENT '工作流状态同步时间',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(128) DEFAULT NULL COMMENT '申请人名称',
  `apply_time` datetime NOT NULL COMMENT '申请时间',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(128) DEFAULT NULL COMMENT '审核人名称',
  `review_reason` varchar(512) DEFAULT NULL COMMENT '审核说明',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_refund_approval_no` (`approval_no`),
  UNIQUE KEY `uk_payment_refund_approval_biz` (`tenant_id`,`app_id`,`biz_refund_no`,`del_flag`),
  KEY `idx_payment_refund_approval_payment` (`tenant_id`,`payment_order_id`,`status`),
  KEY `idx_payment_refund_approval_status` (`tenant_id`,`status`,`created_at`),
  KEY `idx_payment_refund_approval_workflow_apply` (`tenant_id`,`workflow_apply_id`),
  KEY `idx_payment_refund_approval_workflow_instance` (`tenant_id`,`workflow_process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台退款审批';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_refund_order` (
  `id` bigint NOT NULL COMMENT '主键',
  `refund_order_no` varchar(64) NOT NULL COMMENT '退款订单号',
  `biz_refund_no` varchar(64) NOT NULL COMMENT '业务退款号',
  `payment_order_id` bigint NOT NULL COMMENT '支付订单ID',
  `channel_refund_no` varchar(128) DEFAULT NULL COMMENT '通道退款单号',
  `refund_amount` bigint NOT NULL COMMENT '退款金额，单位分',
  `reason` varchar(512) DEFAULT NULL COMMENT '退款原因',
  `status` varchar(32) NOT NULL COMMENT '退款状态',
  `refund_time` datetime DEFAULT NULL COMMENT '退款成功时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_refund_tenant_biz` (`tenant_id`,`biz_refund_no`),
  UNIQUE KEY `uk_payment_refund_no` (`refund_order_no`),
  KEY `idx_payment_refund_payment` (`payment_order_id`),
  KEY `idx_payment_refund_tenant_status_time` (`tenant_id`,`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款订单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_refund_query_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `query_no` varchar(64) NOT NULL COMMENT '退款查询记录号',
  `refund_order_no` varchar(64) NOT NULL COMMENT '退款订单号',
  `biz_refund_no` varchar(64) NOT NULL COMMENT '业务退款单号',
  `pay_order_no` varchar(64) NOT NULL COMMENT '原支付订单号',
  `channel_refund_no` varchar(128) DEFAULT NULL COMMENT '通道退款单号',
  `refund_order_id` bigint NOT NULL COMMENT '退款订单ID',
  `payment_order_id` bigint NOT NULL COMMENT '支付订单ID',
  `business_order_id` bigint NOT NULL COMMENT '业务订单ID',
  `query_type` varchar(32) NOT NULL COMMENT '查询类型',
  `request_payload` varchar(1024) DEFAULT NULL COMMENT '查询请求摘要',
  `response_payload` varchar(2048) DEFAULT NULL COMMENT '查询响应摘要',
  `before_status` varchar(32) NOT NULL COMMENT '查询前状态',
  `channel_status` varchar(32) NOT NULL COMMENT '通道返回状态',
  `result_status` varchar(32) NOT NULL COMMENT '处理后状态',
  `process_result` varchar(64) NOT NULL COMMENT '处理结果',
  `process_message` varchar(512) DEFAULT NULL COMMENT '处理说明',
  `query_time` datetime NOT NULL COMMENT '查询时间',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_refund_query_no` (`query_no`),
  KEY `idx_payment_refund_query_refund` (`tenant_id`,`refund_order_no`,`query_time`),
  KEY `idx_payment_refund_query_payment` (`tenant_id`,`payment_order_id`,`query_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款主动查询记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_risk_rule` (
  `id` bigint NOT NULL COMMENT '主键',
  `rule_code` varchar(64) NOT NULL COMMENT '风控规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '风控规则名称',
  `rule_scope` varchar(32) NOT NULL DEFAULT 'TENANT' COMMENT '规则范围：GLOBAL、TENANT、APP、SUBJECT、METHOD',
  `app_id` bigint DEFAULT NULL COMMENT '应用ID',
  `subject_id` bigint DEFAULT NULL COMMENT '企业主体ID',
  `method_code` varchar(64) DEFAULT NULL COMMENT '标准支付方式编码',
  `risk_type` varchar(32) NOT NULL COMMENT '风控类型',
  `threshold_amount` bigint DEFAULT NULL COMMENT '阈值金额，单位分',
  `period_type` varchar(32) DEFAULT NULL COMMENT '统计周期',
  `period_limit_count` int DEFAULT NULL COMMENT '周期限制笔数',
  `period_limit_amount` bigint DEFAULT NULL COMMENT '周期限制金额，单位分',
  `action_type` varchar(32) NOT NULL DEFAULT 'REJECT' COMMENT '动作：REJECT、REVIEW、WARN',
  `priority` int NOT NULL DEFAULT '100' COMMENT '优先级',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_risk_rule_code` (`tenant_id`,`rule_code`,`del_flag`),
  KEY `idx_payment_risk_rule_scope` (`tenant_id`,`rule_scope`,`status`,`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付基础风控规则';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_settlement_summary` (
  `id` bigint NOT NULL COMMENT '主键',
  `settlement_date` date NOT NULL COMMENT '结算日期',
  `app_code` varchar(64) NOT NULL DEFAULT '' COMMENT '应用编码',
  `enterprise_subject_id` bigint NOT NULL COMMENT '企业主体ID',
  `channel_code` varchar(32) NOT NULL COMMENT '通道编码',
  `trade_amount` bigint NOT NULL DEFAULT '0' COMMENT '交易金额，单位分',
  `refund_amount` bigint NOT NULL DEFAULT '0' COMMENT '退款金额，单位分',
  `fee_amount` bigint NOT NULL DEFAULT '0' COMMENT '手续费金额，单位分',
  `net_amount` bigint NOT NULL DEFAULT '0' COMMENT '净结算金额，单位分',
  `trade_count` int NOT NULL DEFAULT '0' COMMENT '支付成功笔数',
  `refund_count` int NOT NULL DEFAULT '0' COMMENT '退款成功笔数',
  `unresolved_difference_count` int NOT NULL DEFAULT '0' COMMENT '未解决差异笔数',
  `unresolved_difference_amount` bigint NOT NULL DEFAULT '0' COMMENT '未解决差异金额，单位分',
  `status` varchar(32) NOT NULL DEFAULT 'GENERATED' COMMENT '状态：GENERATED、CONFIRMED、VOIDED',
  `generated_by` bigint DEFAULT NULL COMMENT '生成人ID',
  `generated_by_name` varchar(128) DEFAULT NULL COMMENT '生成人名称',
  `generated_at` datetime DEFAULT NULL COMMENT '生成时间',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人ID',
  `confirmed_by_name` varchar(128) DEFAULT NULL COMMENT '确认人名称',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `voided_by` bigint DEFAULT NULL COMMENT '作废人ID',
  `voided_by_name` varchar(128) DEFAULT NULL COMMENT '作废人名称',
  `voided_at` datetime DEFAULT NULL COMMENT '作废时间',
  `void_reason` varchar(512) DEFAULT NULL COMMENT '作废原因',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  KEY `idx_payment_settlement_tenant_date` (`tenant_id`,`settlement_date`),
  KEY `idx_payment_settlement_scope` (`tenant_id`,`settlement_date`,`app_code`,`enterprise_subject_id`,`channel_code`),
  KEY `idx_payment_settlement_tenant_status_date` (`tenant_id`,`status`,`settlement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付结算汇总';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_subject_bank_account` (
  `id` bigint NOT NULL COMMENT '主键',
  `subject_id` bigint NOT NULL COMMENT '企业主体ID',
  `account_name` varchar(128) NOT NULL COMMENT '账户户名',
  `account_no` varchar(512) NOT NULL COMMENT '银行账号密文或受控值',
  `bank_name` varchar(128) NOT NULL COMMENT '开户行名称',
  `bank_branch_name` varchar(256) DEFAULT NULL COMMENT '开户支行名称',
  `bank_code` varchar(64) DEFAULT NULL COMMENT '银行编码',
  `account_type` varchar(32) NOT NULL DEFAULT 'CORPORATE' COMMENT '账户类型：CORPORATE、PERSONAL',
  `default_account` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认账户',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_subject_bank_account` (`tenant_id`,`subject_id`,`account_no`,`del_flag`),
  KEY `idx_payment_subject_bank_account_subject` (`tenant_id`,`subject_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付主体银行账户';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_tenant` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_code` varchar(64) NOT NULL COMMENT '租户编码',
  `tenant_name` varchar(128) NOT NULL COMMENT '租户名称',
  `platform_tenant_id` bigint NOT NULL COMMENT '平台租户ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_tenant_code` (`tenant_code`,`del_flag`),
  UNIQUE KEY `uk_payment_tenant_platform` (`platform_tenant_id`,`del_flag`),
  KEY `idx_payment_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付租户';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_transaction_flow` (
  `id` bigint NOT NULL COMMENT '主键',
  `flow_no` varchar(64) NOT NULL COMMENT '流水号',
  `business_order_id` bigint DEFAULT NULL COMMENT '业务订单ID',
  `payment_order_id` bigint DEFAULT NULL COMMENT '支付订单ID',
  `refund_order_id` bigint DEFAULT NULL COMMENT '退款订单ID',
  `flow_type` varchar(32) NOT NULL COMMENT '流水类型',
  `amount` bigint NOT NULL COMMENT '金额，单位分',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_flow_no` (`flow_no`),
  KEY `idx_payment_flow_tenant_time` (`tenant_id`,`created_at`),
  KEY `idx_payment_flow_tenant_type_audit_time` (`tenant_id`,`flow_type`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易流水';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_virtual_channel_payment` (
  `id` bigint NOT NULL COMMENT '主键',
  `virtual_payment_no` varchar(64) NOT NULL COMMENT '内置虚拟通道支付单号',
  `pay_order_no` varchar(64) DEFAULT NULL COMMENT '支付订单号',
  `channel_trade_no` varchar(128) DEFAULT NULL COMMENT '通道交易号',
  `cashier_config_id` bigint NOT NULL COMMENT '收银台配置ID',
  `payment_method_id` bigint DEFAULT NULL COMMENT '支付方式ID',
  `title` varchar(128) NOT NULL COMMENT '付款标题',
  `amount` bigint NOT NULL COMMENT '付款金额，单位分',
  `payer_name` varchar(128) DEFAULT NULL COMMENT '付款人',
  `status` varchar(32) NOT NULL COMMENT '支付状态',
  `paid_time` datetime DEFAULT NULL COMMENT '完成时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `payment_method_code` varchar(64) DEFAULT NULL COMMENT '标准支付方式编码',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_virtual_payment_no` (`virtual_payment_no`),
  UNIQUE KEY `uk_payment_virtual_channel_trade` (`tenant_id`,`channel_trade_no`),
  UNIQUE KEY `uk_payment_virtual_pay_order` (`tenant_id`,`pay_order_no`),
  KEY `idx_payment_virtual_tenant_cashier` (`tenant_id`,`cashier_config_id`,`created_at`),
  KEY `idx_payment_virtual_tenant_audit_time` (`tenant_id`,`created_at`),
  KEY `idx_payment_virtual_pay_order` (`tenant_id`,`pay_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内置虚拟通道支付记录';
/*!40101 SET character_set_client = @saved_cs_client */;
