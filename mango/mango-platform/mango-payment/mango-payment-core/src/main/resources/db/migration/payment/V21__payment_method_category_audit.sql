ALTER TABLE `payment_method`
  RENAME COLUMN `create_by` TO `created_by`,
  RENAME COLUMN `update_by` TO `updated_by`,
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

UPDATE `payment_method`
SET `created_by` = NULL
WHERE `created_by` IS NOT NULL
  AND `created_by` NOT REGEXP '^[0-9]+$';

UPDATE `payment_method`
SET `updated_by` = NULL
WHERE `updated_by` IS NOT NULL
  AND `updated_by` NOT REGEXP '^[0-9]+$';

ALTER TABLE `payment_method`
  MODIFY COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  MODIFY COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  MODIFY COLUMN `interaction_type` varchar(32) NOT NULL DEFAULT 'QR_CODE' COMMENT '三级分类：扫码/H5/网银跳转/账号转账/快捷等',
  ADD COLUMN `requires_bank_selection` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要银行列表：0-否，1-是' AFTER `icon_file_id`,
  ADD COLUMN `requires_qr_refresh` tinyint NOT NULL DEFAULT '0' COMMENT '二维码是否支持刷新：0-否，1-是' AFTER `requires_bank_selection`;

CREATE TABLE IF NOT EXISTS `payment_method_category` (
  `id` bigint NOT NULL COMMENT '主键',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `category_name` varchar(128) NOT NULL COMMENT '分类名称',
  `level` tinyint NOT NULL COMMENT '层级：1-账户属性，2-支付工具/网络，3-交互/产品形态',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级分类 ID，根节点为 0',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户 ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_method_category` (`tenant_id`, `level`, `parent_id`, `category_code`, `del_flag`),
  KEY `idx_payment_method_category_parent` (`tenant_id`, `parent_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式三级分类字典';

INSERT INTO `payment_method_category`
  (`id`, `category_code`, `category_name`, `level`, `parent_id`, `sort`, `status`, `tenant_id`, `created_by`, `updated_by`, `created_at`, `updated_at`, `del_flag`)
VALUES
  (360001, 'PERSONAL', '对私', 1, 0, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360002, 'CORPORATE', '对公', 1, 0, 20, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360101, 'WECHAT', '微信', 2, 360001, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360102, 'ALIPAY', '支付宝', 2, 360001, 20, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360103, 'UNIONPAY', '银联', 2, 360001, 30, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360104, 'BANK_CARD', '银行卡', 2, 360001, 40, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360105, 'WALLET', '钱包', 2, 360001, 50, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360201, 'EBANK', '网银', 2, 360002, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360202, 'OFFLINE_TRANSFER', '线下转账', 2, 360002, 20, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360301, 'QR_CODE', '扫码', 3, 360101, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360302, 'H5_REDIRECT', 'H5 跳转', 3, 360101, 20, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360303, 'MINIAPP', '小程序', 3, 360101, 30, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360311, 'QR_CODE', '扫码', 3, 360102, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360312, 'H5_REDIRECT', 'H5 跳转', 3, 360102, 20, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360313, 'MINIAPP', '小程序', 3, 360102, 30, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360321, 'QR_CODE', '扫码', 3, 360103, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360331, 'DEBIT_QUICK', '储蓄卡快捷', 3, 360104, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360332, 'CREDIT_QUICK', '信用卡快捷', 3, 360104, 20, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360341, 'WALLET_QUICK', '钱包快捷', 3, 360105, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360351, 'BANK_GATEWAY', '网银跳转', 3, 360201, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0),
  (360361, 'ACCOUNT_TRANSFER', '账号转账', 3, 360202, 10, 1, 1, NULL, NULL, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `category_name` = VALUES(`category_name`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `updated_at` = NOW(),
  `del_flag` = 0;

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'WECHAT',
    `interaction_type` = 'QR_CODE',
    `payment_material_type` = 'QR',
    `requires_bank_selection` = 0,
    `requires_qr_refresh` = 1,
    `updated_at` = NOW()
WHERE `method_code` = 'PERSONAL_WECHAT_QR';

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'ALIPAY',
    `interaction_type` = 'H5_REDIRECT',
    `payment_material_type` = 'H5_PARAM',
    `requires_bank_selection` = 0,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `method_code` = 'PERSONAL_ALIPAY_H5';

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'UNIONPAY',
    `interaction_type` = 'QR_CODE',
    `payment_material_type` = 'QR',
    `requires_bank_selection` = 0,
    `requires_qr_refresh` = 1,
    `updated_at` = NOW()
WHERE `method_code` = 'PERSONAL_UNIONPAY_QR';

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'BANK_CARD',
    `interaction_type` = 'DEBIT_QUICK',
    `payment_material_type` = 'HTML_FORM',
    `requires_bank_selection` = 1,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `method_code` = 'PERSONAL_DEBIT_QUICK';

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'BANK_CARD',
    `interaction_type` = 'CREDIT_QUICK',
    `payment_material_type` = 'HTML_FORM',
    `requires_bank_selection` = 1,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `method_code` = 'PERSONAL_CREDIT_QUICK';

UPDATE `payment_method`
SET `account_nature` = 'PERSONAL',
    `instrument_type` = 'WALLET',
    `interaction_type` = 'WALLET_QUICK',
    `payment_material_type` = 'H5_PARAM',
    `requires_bank_selection` = 0,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `method_code` = 'PERSONAL_WALLET_QUICK';

UPDATE `payment_method`
SET `account_nature` = 'CORPORATE',
    `instrument_type` = 'EBANK',
    `interaction_type` = 'BANK_GATEWAY',
    `payment_material_type` = 'HTML_FORM',
    `requires_bank_selection` = 1,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `method_code` = 'CORPORATE_EBANK_REDIRECT';

UPDATE `payment_method`
SET `account_nature` = 'CORPORATE',
    `instrument_type` = 'OFFLINE_TRANSFER',
    `interaction_type` = 'ACCOUNT_TRANSFER',
    `payment_material_type` = 'TRANSFER_ACCOUNT',
    `requires_bank_selection` = 0,
    `requires_qr_refresh` = 0,
    `updated_at` = NOW()
WHERE `method_code` = 'CORPORATE_OFFLINE_ACCOUNT';
