CREATE TABLE IF NOT EXISTS `mango_pay_manage_domain` (
  `id` bigint NOT NULL COMMENT '管理域ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `code` varchar(64) NOT NULL COMMENT '管理域编码',
  `title` varchar(64) NOT NULL COMMENT '管理域名称',
  `description` varchar(255) NOT NULL COMMENT '管理域说明',
  `badge` varchar(32) NOT NULL COMMENT '短标签',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示顺序',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '标准创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '标准更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mango_pay_manage_domain_code` (`tenant_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付管理域';

CREATE TABLE IF NOT EXISTS `mango_pay_manage_item` (
  `id` bigint NOT NULL COMMENT '配置项ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `domain` varchar(64) NOT NULL COMMENT '管理域编码',
  `code` varchar(64) NOT NULL COMMENT '配置项编码',
  `name` varchar(128) NOT NULL COMMENT '配置项名称',
  `owner` varchar(128) NOT NULL COMMENT '配置归属',
  `status` varchar(32) NOT NULL COMMENT '配置状态',
  `primary_text` varchar(255) NOT NULL COMMENT '主要配置摘要',
  `secondary_text` varchar(255) NOT NULL COMMENT '辅助说明',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示顺序',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '标准创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '标准更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mango_pay_manage_item_code` (`tenant_id`,`domain`,`code`),
  KEY `idx_mango_pay_manage_item_domain` (`tenant_id`,`domain`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付管理配置项';

CREATE TABLE IF NOT EXISTS `mango_pay_method_config` (
  `id` bigint NOT NULL COMMENT '支付方式ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `method_code` varchar(64) NOT NULL COMMENT '支付方式编码',
  `method_name` varchar(64) NOT NULL COMMENT '支付方式名称',
  `channel_code` varchar(64) NOT NULL COMMENT '支付通道编码',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `single_limit` bigint NOT NULL DEFAULT '0' COMMENT '单笔限额，单位分',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示顺序',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '标准创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '标准更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mango_pay_method_code` (`tenant_id`,`method_code`),
  KEY `idx_mango_pay_method_channel` (`tenant_id`,`channel_code`,`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式配置';

CREATE TABLE IF NOT EXISTS `mango_pay_tenant_cashier` (
  `id` bigint NOT NULL COMMENT '收银台ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `tenant_name` varchar(128) NOT NULL COMMENT '租户名称',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码',
  `cashier_code` varchar(64) NOT NULL COMMENT '收银台编码',
  `cashier_name` varchar(128) NOT NULL COMMENT '收银台名称',
  `enabled_methods` varchar(255) NOT NULL COMMENT '启用支付方式，逗号分隔',
  `default_method` varchar(64) NOT NULL COMMENT '默认支付方式',
  `expire_minutes` int NOT NULL DEFAULT '30' COMMENT '订单过期分钟数',
  `daily_limit` bigint NOT NULL DEFAULT '0' COMMENT '日限额，单位分',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '标准创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '标准更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mango_pay_tenant_cashier` (`tenant_id`,`app_code`,`cashier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户收银台配置';

INSERT INTO `mango_pay_manage_domain` (`id`,`code`,`title`,`description`,`badge`,`sort_order`) VALUES
(1001,'overview','总览','支付平台能力、租户收银台和沙箱链路','总览',10),
(1002,'applications','应用管理','接入应用、密钥、回调地址和权限','接入',20),
(1003,'subjects','企业主体','收款主体、证照、银行账户和结算归属','主体',30),
(1004,'channels','支付通道','沙箱、微信、支付宝、通联、连连通道配置','通道',40),
(1005,'methods','支付方式','扫码、收银台、网银、线下方式和限额','方式',50),
(1006,'cashiers','收银台配置','租户收银台样式、可用方式和过期时间','收银台',60),
(1007,'orders','订单与流水','业务单、支付单、交易流水和异常订单','订单',70),
(1008,'refunds','退款管理','退款申请、退款单、刷新和状态追踪','退款',80),
(1009,'notifies','通知管理','渠道回调、业务通知、重试和补偿','通知',90),
(1010,'reconcile','对账管理','渠道账单、自动核对和差异处理','对账',100),
(1011,'settlement','结算汇总','按日、租户、主体、通道汇总资金','结算',110),
(1012,'audit','审计管理','配置变更、人工补偿和资金操作留痕','审计',120)
ON DUPLICATE KEY UPDATE
`title`=VALUES(`title`),`description`=VALUES(`description`),`badge`=VALUES(`badge`),`sort_order`=VALUES(`sort_order`);

INSERT INTO `mango_pay_manage_item` (`id`,`domain`,`code`,`name`,`owner`,`status`,`primary_text`,`secondary_text`,`sort_order`,`updated_at`) VALUES
(2001,'applications','mango-admin','管理后台接入应用','芒果集团','ENABLED','回调白名单 2 个','支付、退款、查单已开通',10,'2026-05-24 09:00:00'),
(2002,'applications','mango-demo','沙箱演示应用','演示租户','ENABLED','仅沙箱通道','用于验收和联调',20,'2026-05-24 09:10:00'),
(2003,'subjects','MANGO-GROUP','芒果集团主体','芒果集团','ENABLED','对公账户已校验','结算报表默认归属主体',10,'2026-05-23 16:20:00'),
(2004,'channels','SANDBOX','沙箱支付通道','平台','ENABLED','无外部网络依赖','支持支付、退款、回调验签',10,'2026-05-24 09:30:00'),
(2005,'channels','WECHAT','微信支付通道','平台','DISABLED','未启用','当前环境仅启用沙箱通道',20,'2026-05-21 12:00:00'),
(2006,'channels','ALIPAY','支付宝支付通道','平台','DISABLED','未启用','当前环境仅启用沙箱通道',30,'2026-05-21 12:00:00'),
(2007,'channels','ALLINPAY','通联支付通道','平台','DISABLED','未启用','当前环境仅启用沙箱通道',40,'2026-05-21 12:00:00'),
(2008,'channels','LIANLIAN','连连支付通道','平台','DISABLED','未启用','当前环境仅启用沙箱通道',50,'2026-05-21 12:00:00'),
(2009,'methods','SANDBOX_QR','沙箱扫码','全部租户','ENABLED','单笔 50,000.00','默认展示在收银台首位',10,'2026-05-24 09:20:00'),
(2010,'methods','SANDBOX_CASHIER','沙箱收银台','全部租户','ENABLED','单笔 50,000.00','用于完整收银台验收流程',20,'2026-05-24 09:22:00'),
(2011,'methods','SANDBOX_BANK','沙箱网银','芒果集团','ENABLED','单笔 100,000.00','企业网银沙箱支付材料',30,'2026-05-24 09:23:00'),
(2012,'cashiers','CASHIER_STANDARD','标准收银台','芒果集团','ENABLED','3 个支付方式','订单 30 分钟过期',10,'2026-05-24 09:25:00'),
(2013,'cashiers','CASHIER_BRANCH','分支机构收银台','华南事业部','ENABLED','2 个支付方式','订单 20 分钟过期',20,'2026-05-24 09:26:00'),
(2014,'cashiers','CASHIER_DEMO','演示沙箱收银台','演示租户','ENABLED','1 个支付方式','订单 15 分钟过期',30,'2026-05-24 09:27:00'),
(2015,'orders','PAY_ORDER_FLOW','支付订单状态机','平台','ENABLED','一单多支付尝试','同一业务单仅允许一笔成功支付',10,'2026-05-24 09:28:00'),
(2016,'refunds','REFUND_FLOW','退款状态机','平台','ENABLED','商户退款号幂等','累计退款金额不超过已支付金额',10,'2026-05-24 09:32:00'),
(2017,'notifies','PAYMENT_NOTIFY','支付结果通知','平台','ENABLED','幂等事件 ID','失败可补偿推送',10,'2026-05-24 09:35:00'),
(2018,'reconcile','SANDBOX_DAILY','沙箱日账单核对','平台','ENABLED','沙箱账单维度','按租户、应用、通道核对',10,'2026-05-21 12:00:00'),
(2019,'settlement','TENANT_DAILY','租户日结算汇总','平台财务','ENABLED','日维度汇总','支付/退款/净收款维度',10,'2026-05-21 12:00:00'),
(2020,'audit','FUND_OPERATION','资金操作审计','风控','ENABLED','人工纠偏留痕','记录操作者、动作和原因',10,'2026-05-21 12:00:00')
ON DUPLICATE KEY UPDATE
`name`=VALUES(`name`),`owner`=VALUES(`owner`),`status`=VALUES(`status`),`primary_text`=VALUES(`primary_text`),`secondary_text`=VALUES(`secondary_text`),`sort_order`=VALUES(`sort_order`),`updated_at`=VALUES(`updated_at`);

INSERT INTO `mango_pay_method_config` (`id`,`method_code`,`method_name`,`channel_code`,`status`,`single_limit`,`sort_order`) VALUES
(3001,'SANDBOX_QR','沙箱扫码','SANDBOX','ENABLED',5000000,10),
(3002,'SANDBOX_CASHIER','沙箱收银台','SANDBOX','ENABLED',5000000,20),
(3003,'SANDBOX_BANK','沙箱网银','SANDBOX','ENABLED',10000000,30)
ON DUPLICATE KEY UPDATE
`method_name`=VALUES(`method_name`),`channel_code`=VALUES(`channel_code`),`status`=VALUES(`status`),`single_limit`=VALUES(`single_limit`),`sort_order`=VALUES(`sort_order`);

INSERT INTO `mango_pay_tenant_cashier` (`id`,`tenant_id`,`tenant_name`,`app_code`,`cashier_code`,`cashier_name`,`enabled_methods`,`default_method`,`expire_minutes`,`daily_limit`) VALUES
(4001,1,'芒果集团','mango-admin','CASHIER_STANDARD','标准收银台','SANDBOX_QR,SANDBOX_CASHIER,SANDBOX_BANK','SANDBOX_QR',30,5000000),
(4002,2,'华南事业部','mango-south','CASHIER_BRANCH','分支机构收银台','SANDBOX_QR,SANDBOX_CASHIER','SANDBOX_CASHIER',20,2000000),
(4003,3,'演示租户','mango-demo','CASHIER_DEMO','演示沙箱收银台','SANDBOX_QR','SANDBOX_QR',15,500000)
ON DUPLICATE KEY UPDATE
`tenant_name`=VALUES(`tenant_name`),`cashier_name`=VALUES(`cashier_name`),`enabled_methods`=VALUES(`enabled_methods`),`default_method`=VALUES(`default_method`),`expire_minutes`=VALUES(`expire_minutes`),`daily_limit`=VALUES(`daily_limit`);
