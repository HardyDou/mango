ALTER TABLE `payment_application`
  ADD COLUMN `app_secret` varchar(128) DEFAULT NULL COMMENT '应用密钥',
  ADD COLUMN `sign_algorithm` varchar(32) NOT NULL DEFAULT 'HMAC_SHA256' COMMENT '签名算法',
  ADD COLUMN `ip_whitelist` varchar(1024) DEFAULT NULL COMMENT 'IP 白名单',
  ADD COLUMN `notify_url_whitelist` varchar(1024) DEFAULT NULL COMMENT '通知地址白名单',
  ADD COLUMN `return_domain_whitelist` varchar(1024) DEFAULT NULL COMMENT '返回域名白名单',
  ADD COLUMN `payload_encrypt_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '报文加密开关',
  ADD COLUMN `default_cashier_id` bigint DEFAULT NULL COMMENT '默认收银台配置 ID';

ALTER TABLE `payment_channel`
  ADD COLUMN `channel_type` varchar(32) NOT NULL DEFAULT 'AGGREGATOR' COMMENT '通道类型',
  ADD COLUMN `adapter_type` varchar(64) NOT NULL DEFAULT 'UNCONFIGURED' COMMENT '适配器类型',
  ADD COLUMN `gateway_base_url` varchar(512) DEFAULT NULL COMMENT '基础网关地址',
  ADD COLUMN `field_template_json` text DEFAULT NULL COMMENT '签约字段模板 JSON',
  ADD COLUMN `capability_summary` varchar(1024) DEFAULT NULL COMMENT '通道能力摘要';

ALTER TABLE `payment_method`
  MODIFY COLUMN `channel_id` bigint DEFAULT NULL COMMENT '历史字段：支付方式不得直接绑定通道',
  ADD COLUMN `account_nature` varchar(32) NOT NULL DEFAULT 'PERSONAL' COMMENT '一级分类：对公/对私',
  ADD COLUMN `instrument_type` varchar(32) NOT NULL DEFAULT 'WECHAT' COMMENT '二级分类：网银/线下/支付宝/微信/银联等',
  ADD COLUMN `interaction_type` varchar(32) NOT NULL DEFAULT 'QR_CODE' COMMENT '三级交互：扫码/H5/网银跳转/账号转账等',
  ADD COLUMN `terminal_scope` varchar(64) NOT NULL DEFAULT 'WEB,H5' COMMENT '终端范围',
  ADD COLUMN `payment_material_type` varchar(32) NOT NULL DEFAULT 'QR' COMMENT '支付物料类型',
  ADD COLUMN `icon_file_id` bigint DEFAULT NULL COMMENT '图标文件 ID',
  ADD COLUMN `description` varchar(512) DEFAULT NULL COMMENT '收银台说明',
  ADD COLUMN `visible_scope` varchar(512) DEFAULT NULL COMMENT '可见范围',
  ADD COLUMN `route_strategy` varchar(1024) DEFAULT NULL COMMENT '路由策略说明';

ALTER TABLE `payment_cashier_config`
  ADD COLUMN `cashier_code` varchar(64) DEFAULT NULL COMMENT '收银台配置编码',
  ADD COLUMN `app_scope` varchar(512) DEFAULT NULL COMMENT '适用应用范围',
  ADD COLUMN `subject_scope` varchar(512) DEFAULT NULL COMMENT '适用主体范围',
  ADD COLUMN `terminal_scope` varchar(64) DEFAULT NULL COMMENT '终端范围',
  ADD COLUMN `method_codes` varchar(1024) DEFAULT NULL COMMENT '可见标准支付方式编码',
  ADD COLUMN `default_method_code` varchar(64) DEFAULT NULL COMMENT '默认标准支付方式编码',
  ADD COLUMN `method_display_order` varchar(1024) DEFAULT NULL COMMENT '支付方式展示顺序',
  ADD COLUMN `theme_config` text DEFAULT NULL COMMENT '主题配置 JSON',
  ADD COLUMN `layout_config` text DEFAULT NULL COMMENT '布局配置 JSON',
  ADD COLUMN `timeout_config` text DEFAULT NULL COMMENT '超时配置 JSON',
  ADD COLUMN `result_config` text DEFAULT NULL COMMENT '结果展示配置 JSON',
  ADD COLUMN `bank_display_config` text DEFAULT NULL COMMENT '网银银行列表配置 JSON',
  ADD COLUMN `offline_transfer_config` text DEFAULT NULL COMMENT '线下转账配置 JSON';

ALTER TABLE `payment_virtual_channel_payment`
  ADD COLUMN `payment_method_code` varchar(64) DEFAULT NULL COMMENT '标准支付方式编码';

CREATE TABLE IF NOT EXISTS `payment_channel_contract` (
  `id` bigint NOT NULL COMMENT '主键',
  `contract_code` varchar(64) NOT NULL COMMENT '签约编码',
  `contract_name` varchar(128) NOT NULL COMMENT '签约名称',
  `subject_id` bigint NOT NULL COMMENT '企业主体 ID',
  `channel_id` bigint NOT NULL COMMENT '支付通道 ID',
  `environment` varchar(32) NOT NULL COMMENT '签约环境',
  `merchant_no` varchar(64) DEFAULT NULL COMMENT '商户号',
  `app_id` varchar(128) DEFAULT NULL COMMENT '通道 AppId',
  `config_values_json` text DEFAULT NULL COMMENT '按通道字段模板填写的配置值 JSON',
  `enabled_method_codes` varchar(1024) DEFAULT NULL COMMENT '已开通标准支付方式编码',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_contract_tenant_code` (`tenant_id`, `contract_code`, `del_flag`),
  KEY `idx_payment_contract_subject` (`tenant_id`, `subject_id`, `status`),
  KEY `idx_payment_contract_channel` (`tenant_id`, `channel_id`, `environment`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道签约配置';

CREATE TABLE IF NOT EXISTS `payment_channel_capability` (
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
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_channel_capability` (`tenant_id`, `channel_id`, `method_code`, `terminal_type`, `environment`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通道能力';

CREATE TABLE IF NOT EXISTS `payment_channel_contract_capability` (
  `id` bigint NOT NULL COMMENT '主键',
  `contract_id` bigint NOT NULL COMMENT '通道签约 ID',
  `channel_capability_id` bigint NOT NULL COMMENT '通道能力 ID',
  `method_code` varchar(64) NOT NULL COMMENT '标准支付方式编码',
  `terminal_type` varchar(32) NOT NULL COMMENT '终端类型',
  `fee_rate` decimal(10,6) DEFAULT NULL COMMENT '费率',
  `min_amount` bigint DEFAULT NULL COMMENT '最小金额，单位分',
  `max_amount` bigint DEFAULT NULL COMMENT '最大金额，单位分',
  `priority` int NOT NULL DEFAULT '100' COMMENT '路由优先级',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_contract_capability` (`tenant_id`, `contract_id`, `channel_capability_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付签约能力';

CREATE TABLE IF NOT EXISTS `payment_method_route_rule` (
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
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_route_rule` (`tenant_id`, `rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式路由规则';

CREATE TABLE IF NOT EXISTS `payment_method_route_rule_item` (
  `id` bigint NOT NULL COMMENT '主键',
  `rule_id` bigint NOT NULL COMMENT '路由规则 ID',
  `contract_capability_id` bigint NOT NULL COMMENT '签约能力 ID',
  `priority` int NOT NULL DEFAULT '100' COMMENT '优先级',
  `weight` int NOT NULL DEFAULT '100' COMMENT '权重',
  `min_amount` bigint DEFAULT NULL COMMENT '最小金额，单位分',
  `max_amount` bigint DEFAULT NULL COMMENT '最大金额，单位分',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_route_rule_item` (`tenant_id`, `rule_id`, `contract_capability_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式路由规则明细';

UPDATE `payment_channel`
SET `channel_name` = '芒果支付',
    `channel_type` = 'BUILTIN_VIRTUAL',
    `adapter_type` = 'MANGO_PAY',
    `environment` = 'CHANNEL_PRODUCT',
    `merchant_no` = 'MANGO_PAY_MERCHANT_001',
    `gateway_url` = '/payment/mango-pay/virtual',
    `gateway_base_url` = '/payment/mango-pay/virtual',
    `field_template_json` = '[{"name":"merchantNo","label":"商户号","component":"input","required":true},{"name":"mangoPayScenario","label":"支付场景控制","component":"textarea","required":false}]',
    `capability_summary` = '芒果支付内置虚拟通道，支持全部标准支付方式、支付下单、回调、查单、关单、退款、退款查询、账单、对账、返回码映射和异常场景控制'
WHERE `channel_code` = 'MANGO_PAY';

UPDATE `payment_method`
SET `method_code` = 'PERSONAL_WECHAT_QR',
    `method_name` = '微信扫码',
    `channel_id` = NULL,
    `account_nature` = 'PERSONAL',
    `instrument_type` = 'WECHAT',
    `interaction_type` = 'QR_CODE',
    `terminal_scope` = 'WEB,H5',
    `payment_material_type` = 'QR',
    `description` = '桌面 Web 展示二维码，H5 展示扫码或跳转引导',
    `route_strategy` = '按租户、应用、主体、终端、金额命中签约能力'
WHERE `id` = 340001;

UPDATE `payment_method`
SET `method_code` = 'CORPORATE_OFFLINE_ACCOUNT',
    `method_name` = '对公转账',
    `channel_id` = NULL,
    `account_nature` = 'CORPORATE',
    `instrument_type` = 'OFFLINE_TRANSFER',
    `interaction_type` = 'OFFLINE_TRANSFER',
    `terminal_scope` = 'WEB,H5',
    `payment_material_type` = 'TRANSFER_ACCOUNT',
    `description` = '展示收款户名、账号、开户行、转账备注和认款说明',
    `route_strategy` = '路由到支持对公转账的签约能力'
WHERE `id` = 340002;

UPDATE `payment_method`
SET `method_code` = 'PERSONAL_ALIPAY_H5',
    `method_name` = '支付宝 H5',
    `channel_id` = NULL,
    `account_nature` = 'PERSONAL',
    `instrument_type` = 'ALIPAY',
    `interaction_type` = 'H5_REDIRECT',
    `terminal_scope` = 'H5',
    `payment_material_type` = 'H5_PARAM',
    `description` = '移动 Web 跳转支付宝完成付款',
    `route_strategy` = '路由到通联或支付宝直连签约能力'
WHERE `id` = 340003;

UPDATE `payment_method`
SET `method_code` = 'CORPORATE_EBANK_REDIRECT',
    `method_name` = '企业网银',
    `channel_id` = NULL,
    `account_nature` = 'CORPORATE',
    `instrument_type` = 'EBANK',
    `interaction_type` = 'BANK_GATEWAY',
    `terminal_scope` = 'WEB',
    `payment_material_type` = 'HTML_FORM',
    `description` = '选择银行后跳转企业网银授权付款',
    `route_strategy` = '路由到华夏银行或通联企业网银签约能力'
WHERE `id` = 340004;

INSERT INTO `payment_channel_contract`
  (`id`, `contract_code`, `contract_name`, `subject_id`, `channel_id`, `environment`, `merchant_no`, `app_id`, `config_values_json`, `enabled_method_codes`, `status`, `tenant_id`, `create_by`, `update_by`)
VALUES
  (331001, 'MANGO_PAY_MANGO_TECH', '芒果科技芒果支付签约', 320001, 330001, 'MANGO_PAY', 'MANGO_PAY_MERCHANT_001', 'mango-pay-app', '{"mangoPayScenario":"SUCCESS"}', 'PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT,PERSONAL_ALIPAY_H5,CORPORATE_EBANK_REDIRECT', 1, 1, 'system', 'system'),
  (331002, 'ALLINPAY_MANGO_TECH', '芒果科技通联签约', 320001, 330002, 'PROD', NULL, NULL, '{}', NULL, 0, 1, 'system', 'system')
ON DUPLICATE KEY UPDATE
  `contract_name` = VALUES(`contract_name`),
  `enabled_method_codes` = VALUES(`enabled_method_codes`),
  `status` = VALUES(`status`),
  `update_time` = NOW();

INSERT INTO `payment_channel_capability`
  (`id`, `channel_id`, `method_code`, `terminal_type`, `environment`, `min_amount`, `max_amount`, `tenant_id`)
VALUES
  (332001, 330001, 'PERSONAL_WECHAT_QR', 'WEB', 'MANGO_PAY', 1, 5000000, 1),
  (332002, 330001, 'CORPORATE_OFFLINE_ACCOUNT', 'WEB', 'MANGO_PAY', 1, 20000000, 1),
  (332003, 330001, 'PERSONAL_ALIPAY_H5', 'H5', 'MANGO_PAY', 1, 5000000, 1),
  (332004, 330001, 'CORPORATE_EBANK_REDIRECT', 'WEB', 'MANGO_PAY', 1, 20000000, 1),
  (332005, 330002, 'PERSONAL_WECHAT_QR', 'WEB', 'PROD', 1, 5000000, 1),
  (332006, 330002, 'PERSONAL_ALIPAY_H5', 'H5', 'PROD', 1, 5000000, 1)
ON DUPLICATE KEY UPDATE
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `status` = 1,
  `update_time` = NOW();

INSERT INTO `payment_channel_contract_capability`
  (`id`, `contract_id`, `channel_capability_id`, `method_code`, `terminal_type`, `min_amount`, `max_amount`, `priority`, `tenant_id`)
VALUES
  (333001, 331001, 332001, 'PERSONAL_WECHAT_QR', 'WEB', 1, 5000000, 10, 1),
  (333002, 331001, 332002, 'CORPORATE_OFFLINE_ACCOUNT', 'WEB', 1, 20000000, 20, 1),
  (333003, 331001, 332003, 'PERSONAL_ALIPAY_H5', 'H5', 1, 5000000, 30, 1),
  (333004, 331001, 332004, 'CORPORATE_EBANK_REDIRECT', 'WEB', 1, 20000000, 40, 1),
  (333005, 331002, 332005, 'PERSONAL_WECHAT_QR', 'WEB', 1, 5000000, 100, 1),
  (333006, 331002, 332006, 'PERSONAL_ALIPAY_H5', 'H5', 1, 5000000, 100, 1)
ON DUPLICATE KEY UPDATE
  `priority` = VALUES(`priority`),
  `status` = 1,
  `update_time` = NOW();

UPDATE `payment_channel_capability`
SET `status` = 0,
    `update_time` = NOW()
WHERE `channel_id` = 330002;

UPDATE `payment_channel_contract_capability`
SET `status` = 0,
    `update_time` = NOW()
WHERE `contract_id` = 331002;

INSERT INTO `payment_method_route_rule`
  (`id`, `rule_code`, `rule_name`, `app_id`, `subject_id`, `method_code`, `terminal_type`, `environment`, `route_mode`, `fallback_enabled`, `tenant_id`)
VALUES
  (334001, 'ORDER_CENTER_WECHAT_QR_MANGO_PAY', '订单中心微信扫码芒果支付内部验证路由', 310001, 320001, 'PERSONAL_WECHAT_QR', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1),
  (334002, 'ORDER_CENTER_ALIPAY_H5_MANGO_PAY', '订单中心支付宝 H5 芒果支付内部验证路由', 310001, 320001, 'PERSONAL_ALIPAY_H5', 'H5', 'MANGO_PAY', 'PRIORITY', 1, 1)
ON DUPLICATE KEY UPDATE
  `rule_name` = VALUES(`rule_name`),
  `route_mode` = VALUES(`route_mode`),
  `fallback_enabled` = VALUES(`fallback_enabled`),
  `update_time` = NOW();

INSERT INTO `payment_method_route_rule_item`
  (`id`, `rule_id`, `contract_capability_id`, `priority`, `weight`, `min_amount`, `max_amount`, `tenant_id`)
VALUES
  (335001, 334001, 333001, 10, 100, 1, 5000000, 1),
  (335002, 334002, 333003, 10, 100, 1, 5000000, 1)
ON DUPLICATE KEY UPDATE
  `priority` = VALUES(`priority`),
  `weight` = VALUES(`weight`),
  `status` = 1,
  `update_time` = NOW();

UPDATE `payment_cashier_config`
SET `cashier_name` = CASE `id`
      WHEN 350001 THEN '订单中心 Web 收银台'
      WHEN 350002 THEN '订单中心 H5 收银台'
      ELSE `cashier_name`
    END,
    `cashier_code` = CASE `id`
      WHEN 350001 THEN 'ORDER_CENTER_WEB'
      WHEN 350002 THEN 'ORDER_CENTER_H5'
      ELSE `cashier_code`
    END,
    `terminal_type` = CASE WHEN `terminal_type` = 'PC' THEN 'WEB' ELSE `terminal_type` END,
    `terminal_scope` = CASE WHEN `terminal_type` = 'PC' THEN 'WEB' ELSE `terminal_type` END,
    `method_codes` = CASE `id`
      WHEN 350001 THEN 'PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT,CORPORATE_EBANK_REDIRECT'
      WHEN 350002 THEN 'PERSONAL_WECHAT_QR,PERSONAL_ALIPAY_H5,CORPORATE_OFFLINE_ACCOUNT'
      ELSE `method_codes`
    END,
    `default_method_code` = CASE `id`
      WHEN 350001 THEN 'PERSONAL_WECHAT_QR'
      WHEN 350002 THEN 'PERSONAL_ALIPAY_H5'
      ELSE `default_method_code`
    END,
    `method_display_order` = CASE `id`
      WHEN 350001 THEN 'PERSONAL_WECHAT_QR,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT'
      WHEN 350002 THEN 'PERSONAL_ALIPAY_H5,PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT'
      ELSE `method_display_order`
    END,
    `theme_config` = '{"brandName":"芒果支付","primaryColor":"#2563eb"}',
    `layout_config` = '{"amountArea":true,"orderInfoArea":true,"helpArea":true}',
    `timeout_config` = '{"expireMinutes":30,"pollingSeconds":3,"qrRefreshSeconds":120}',
    `result_config` = '{"showInlineResult":true,"successText":"支付成功","processingText":"支付处理中"}',
    `bank_display_config` = '{"showBankSearch":true,"groups":["常用银行","全部银行"]}',
    `offline_transfer_config` = '{"accountName":"芒果科技有限公司","accountNo":"622200000000000001","bankName":"招商银行上海分行","remarkPrefix":"PAY"}'
WHERE `id` IN (350001, 350002);

UPDATE `payment_cashier_config`
SET `status` = 0,
    `terminal_type` = 'H5',
    `terminal_scope` = 'H5'
WHERE `id` = 350003;
