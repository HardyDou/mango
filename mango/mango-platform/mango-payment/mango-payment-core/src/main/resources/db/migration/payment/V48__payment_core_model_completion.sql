CREATE TABLE IF NOT EXISTS `payment_tenant` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_code` varchar(64) NOT NULL COMMENT '租户编码',
  `tenant_name` varchar(128) NOT NULL COMMENT '租户名称',
  `platform_tenant_id` bigint NOT NULL COMMENT '平台租户ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_tenant_code` (`tenant_code`, `del_flag`),
  UNIQUE KEY `uk_payment_tenant_platform` (`platform_tenant_id`, `del_flag`),
  KEY `idx_payment_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付租户';

CREATE TABLE IF NOT EXISTS `payment_subject_bank_account` (
  `id` bigint NOT NULL COMMENT '主键',
  `subject_id` bigint NOT NULL COMMENT '企业主体ID',
  `account_name` varchar(128) NOT NULL COMMENT '账户户名',
  `account_no` varchar(128) NOT NULL COMMENT '银行账号密文或受控值',
  `bank_name` varchar(128) NOT NULL COMMENT '开户行名称',
  `bank_branch_name` varchar(256) DEFAULT NULL COMMENT '开户支行名称',
  `bank_code` varchar(64) DEFAULT NULL COMMENT '银行编码',
  `account_type` varchar(32) NOT NULL DEFAULT 'CORPORATE' COMMENT '账户类型：CORPORATE、PERSONAL',
  `default_account` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认账户',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_subject_bank_account` (`tenant_id`, `subject_id`, `account_no`, `del_flag`),
  KEY `idx_payment_subject_bank_account_subject` (`tenant_id`, `subject_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付主体银行账户';

CREATE TABLE IF NOT EXISTS `payment_channel_field_template` (
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
  `option_json` text DEFAULT NULL COMMENT '选项 JSON',
  `validation_json` text DEFAULT NULL COMMENT '校验规则 JSON',
  `field_group` varchar(64) DEFAULT NULL COMMENT '字段分组',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_field_template` (`tenant_id`, `channel_id`, `field_code`, `del_flag`),
  KEY `idx_payment_channel_field_template_channel` (`tenant_id`, `channel_id`, `status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道签约字段模板';

CREATE TABLE IF NOT EXISTS `payment_channel_contract_value` (
  `id` bigint NOT NULL COMMENT '主键',
  `contract_id` bigint NOT NULL COMMENT '通道签约ID',
  `field_code` varchar(64) NOT NULL COMMENT '字段编码',
  `value_text` varchar(1024) DEFAULT NULL COMMENT '非敏感签约值',
  `encrypted_value` text DEFAULT NULL COMMENT '敏感签约密文值',
  `file_id` bigint DEFAULT NULL COMMENT '文件中心ID',
  `value_source` varchar(32) NOT NULL DEFAULT 'CONFIG' COMMENT '值来源：CONFIG、COLUMN、FILE',
  `sensitive_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否敏感',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_contract_value` (`tenant_id`, `contract_id`, `field_code`, `del_flag`),
  KEY `idx_payment_channel_contract_value_contract` (`tenant_id`, `contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道签约配置值';

CREATE TABLE IF NOT EXISTS `payment_channel_bill_batch` (
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
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_bill_batch_file` (`tenant_id`, `channel_code`, `bill_date`, `file_digest`, `del_flag`),
  UNIQUE KEY `uk_payment_channel_bill_batch_no` (`tenant_id`, `batch_no`, `del_flag`),
  KEY `idx_payment_channel_bill_batch_status` (`tenant_id`, `channel_code`, `bill_date`, `import_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道账单批次';

CREATE TABLE IF NOT EXISTS `payment_risk_rule` (
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
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_risk_rule_code` (`tenant_id`, `rule_code`, `del_flag`),
  KEY `idx_payment_risk_rule_scope` (`tenant_id`, `rule_scope`, `status`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付基础风控规则';

INSERT INTO `payment_tenant`
  (`id`, `tenant_code`, `tenant_name`, `platform_tenant_id`, `status`, `tenant_id`, `created_by`, `updated_by`)
VALUES
  (300001, 'TENANT_1', '默认支付租户', 1, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
  `tenant_name` = VALUES(`tenant_name`),
  `platform_tenant_id` = VALUES(`platform_tenant_id`),
  `status` = VALUES(`status`),
  `updated_at` = NOW();

INSERT INTO `payment_subject_bank_account`
  (`id`, `subject_id`, `account_name`, `account_no`, `bank_name`, `account_type`, `default_account`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 1000000000 + ROW_NUMBER() OVER (ORDER BY `id`),
       `id`,
       `subject_name`,
       `bank_account_no`,
       `bank_name`,
       'CORPORATE',
       1,
       `status`,
       `tenant_id`,
       `created_by`,
       `created_at`,
       `updated_by`,
       `updated_at`
FROM `payment_enterprise_subject`
WHERE `del_flag` = 0
  AND `bank_account_no` IS NOT NULL
  AND `bank_name` IS NOT NULL
ON DUPLICATE KEY UPDATE
  `account_name` = VALUES(`account_name`),
  `bank_name` = VALUES(`bank_name`),
  `default_account` = VALUES(`default_account`),
  `status` = VALUES(`status`),
  `updated_at` = VALUES(`updated_at`);

INSERT INTO `payment_channel_field_template`
  (`id`, `channel_id`, `field_code`, `field_label`, `component_type`, `data_type`, `required_flag`, `sensitive_flag`, `encrypted_flag`, `masked_flag`, `file_reference_flag`, `option_json`, `field_group`, `sort`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 2000000000 + ROW_NUMBER() OVER (ORDER BY `channel_id`, `ordinality`),
       `channel_id`,
       `field_code`,
       `field_label`,
       `component_type`,
       `data_type`,
       `required_flag`,
       `sensitive_flag`,
       `encrypted_flag`,
       `masked_flag`,
       `file_reference_flag`,
       `option_json`,
       `field_group`,
       `sort`,
       `status`,
       `tenant_id`,
       `created_by`,
       `created_at`,
       `updated_by`,
       `updated_at`
FROM (
  SELECT c.`id` AS `channel_id`,
         jt.`ordinality`,
         jt.`field_code`,
         COALESCE(jt.`field_label`, jt.`field_code`) AS `field_label`,
         COALESCE(jt.`component_type`, 'input') AS `component_type`,
         COALESCE(jt.`data_type`, 'string') AS `data_type`,
         COALESCE(jt.`required_flag`, 0) AS `required_flag`,
         COALESCE(jt.`sensitive_flag`, 0) AS `sensitive_flag`,
         COALESCE(jt.`encrypted_flag`, 0) AS `encrypted_flag`,
         COALESCE(jt.`masked_flag`, 0) AS `masked_flag`,
         CASE WHEN COALESCE(jt.`data_type`, '') = 'fileId' OR COALESCE(jt.`component_type`, '') = 'fileId' THEN 1 ELSE 0 END AS `file_reference_flag`,
         jt.`option_json`,
         jt.`field_group`,
         COALESCE(jt.`sort`, jt.`ordinality`) AS `sort`,
         c.`status`,
         c.`tenant_id`,
         c.`created_by`,
         c.`created_at`,
         c.`updated_by`,
         c.`updated_at`
  FROM `payment_channel` c
  JOIN JSON_TABLE(
    CASE WHEN JSON_VALID(c.`field_template_json`) THEN c.`field_template_json` ELSE JSON_ARRAY() END,
    '$[*]' COLUMNS (
      `ordinality` FOR ORDINALITY,
      `field_code` varchar(64) PATH '$.name',
      `field_label` varchar(128) PATH '$.label' NULL ON EMPTY,
      `component_type` varchar(64) PATH '$.component' NULL ON EMPTY,
      `data_type` varchar(32) PATH '$.dataType' NULL ON EMPTY,
      `required_flag` tinyint PATH '$.required' DEFAULT '0' ON EMPTY,
      `sensitive_flag` tinyint PATH '$.sensitive' DEFAULT '0' ON EMPTY,
      `encrypted_flag` tinyint PATH '$.encrypted' DEFAULT '0' ON EMPTY,
      `masked_flag` tinyint PATH '$.masked' DEFAULT '0' ON EMPTY,
      `option_json` json PATH '$.options' NULL ON EMPTY,
      `field_group` varchar(64) PATH '$.group' NULL ON EMPTY,
      `sort` int PATH '$.sort' NULL ON EMPTY
    )
  ) jt
  WHERE c.`del_flag` = 0
    AND c.`field_template_json` IS NOT NULL
    AND jt.`field_code` IS NOT NULL
) template_rows
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `component_type` = VALUES(`component_type`),
  `data_type` = VALUES(`data_type`),
  `required_flag` = VALUES(`required_flag`),
  `sensitive_flag` = VALUES(`sensitive_flag`),
  `encrypted_flag` = VALUES(`encrypted_flag`),
  `masked_flag` = VALUES(`masked_flag`),
  `file_reference_flag` = VALUES(`file_reference_flag`),
  `option_json` = VALUES(`option_json`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `updated_at` = VALUES(`updated_at`);

INSERT INTO `payment_channel_contract_value`
  (`id`, `contract_id`, `field_code`, `value_text`, `value_source`, `sensitive_flag`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 3000000000 + ROW_NUMBER() OVER (ORDER BY `contract_id`, `ordinality`),
       `contract_id`,
       `field_code`,
       `value_text`,
       'CONFIG',
       `sensitive_flag`,
       `tenant_id`,
       `created_by`,
       `created_at`,
       `updated_by`,
       `updated_at`
FROM (
  SELECT cc.`id` AS `contract_id`,
         keys_table.`ordinality`,
         keys_table.`field_code`,
         JSON_UNQUOTE(JSON_EXTRACT(cc.`config_values_json`, CONCAT('$.', keys_table.`field_code`))) AS `value_text`,
         CASE WHEN LOWER(keys_table.`field_code`) REGEXP 'secret|private|key|cert|password' THEN 1 ELSE 0 END AS `sensitive_flag`,
         cc.`tenant_id`,
         cc.`created_by`,
         cc.`created_at`,
         cc.`updated_by`,
         cc.`updated_at`
  FROM `payment_channel_contract` cc
  JOIN JSON_TABLE(
    CASE WHEN JSON_VALID(cc.`config_values_json`) THEN JSON_KEYS(cc.`config_values_json`) ELSE JSON_ARRAY() END,
    '$[*]' COLUMNS (
      `ordinality` FOR ORDINALITY,
      `field_code` varchar(64) PATH '$'
    )
  ) keys_table
  WHERE cc.`del_flag` = 0
    AND cc.`config_values_json` IS NOT NULL
    AND keys_table.`field_code` IS NOT NULL
) contract_value_rows
ON DUPLICATE KEY UPDATE
  `value_text` = VALUES(`value_text`),
  `value_source` = VALUES(`value_source`),
  `sensitive_flag` = VALUES(`sensitive_flag`),
  `updated_at` = VALUES(`updated_at`);

INSERT INTO `payment_channel_contract_value`
  (`id`, `contract_id`, `field_code`, `value_text`, `value_source`, `sensitive_flag`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 4000000000 + ROW_NUMBER() OVER (ORDER BY cc.`id`),
       cc.`id`,
       'merchantNo',
       cc.`merchant_no`,
       'COLUMN',
       0,
       cc.`tenant_id`,
       cc.`created_by`,
       cc.`created_at`,
       cc.`updated_by`,
       cc.`updated_at`
FROM `payment_channel_contract` cc
WHERE cc.`del_flag` = 0
  AND cc.`merchant_no` IS NOT NULL
ON DUPLICATE KEY UPDATE
  `value_text` = VALUES(`value_text`),
  `value_source` = VALUES(`value_source`),
  `updated_at` = VALUES(`updated_at`);

INSERT INTO `payment_channel_bill_batch`
  (`id`, `batch_no`, `reconciliation_id`, `channel_code`, `bill_date`, `file_digest`, `bill_file_id`, `bill_file_name`, `total_count`, `total_amount`, `total_fee`, `import_status`, `importer_id`, `importer_name`, `import_time`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 5000000000 + ROW_NUMBER() OVER (ORDER BY `id`),
       `reconciliation_no`,
       `id`,
       `channel_code`,
       `bill_date`,
       `file_digest`,
       `bill_file_id`,
       `bill_file_name`,
       `total_count`,
       `total_amount`,
       `total_fee`,
       'IMPORTED',
       `importer_id`,
       `importer_name`,
       `import_time`,
       `tenant_id`,
       `created_by`,
       `created_at`,
       `updated_by`,
       `updated_at`
FROM `payment_reconciliation`
WHERE `del_flag` = 0
ON DUPLICATE KEY UPDATE
  `reconciliation_id` = VALUES(`reconciliation_id`),
  `total_count` = VALUES(`total_count`),
  `total_amount` = VALUES(`total_amount`),
  `total_fee` = VALUES(`total_fee`),
  `import_status` = VALUES(`import_status`),
  `updated_at` = VALUES(`updated_at`);

INSERT INTO `payment_risk_rule`
  (`id`, `rule_code`, `rule_name`, `rule_scope`, `risk_type`, `threshold_amount`, `action_type`, `priority`, `status`, `tenant_id`, `created_by`, `updated_by`)
VALUES
  (390001, 'DEFAULT_POSITIVE_AMOUNT', '默认支付金额正数校验', 'TENANT', 'AMOUNT_LIMIT', 1, 'REJECT', 10, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
  `rule_name` = VALUES(`rule_name`),
  `threshold_amount` = VALUES(`threshold_amount`),
  `action_type` = VALUES(`action_type`),
  `priority` = VALUES(`priority`),
  `status` = VALUES(`status`),
  `updated_at` = NOW();
