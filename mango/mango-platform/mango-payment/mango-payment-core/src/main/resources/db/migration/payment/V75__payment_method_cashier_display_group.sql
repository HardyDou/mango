ALTER TABLE `payment_method`
  ADD COLUMN `cashier_group_code` varchar(64) DEFAULT NULL COMMENT '收银台展示分组编码' AFTER `payment_material_type`,
  ADD COLUMN `cashier_group_name` varchar(128) DEFAULT NULL COMMENT '收银台展示分组名称' AFTER `cashier_group_code`,
  ADD COLUMN `cashier_group_sort` int NOT NULL DEFAULT '0' COMMENT '收银台展示分组排序' AFTER `cashier_group_name`;

UPDATE `payment_method`
SET `cashier_group_code` = 'WECHAT_PAY',
    `cashier_group_name` = '微信支付',
    `cashier_group_sort` = 10,
    `updated_at` = NOW()
WHERE `method_code` = 'PERSONAL_WECHAT_QR'
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `cashier_group_code` = 'ALIPAY',
    `cashier_group_name` = '支付宝',
    `cashier_group_sort` = 20,
    `updated_at` = NOW()
WHERE `method_code` IN ('PERSONAL_ALIPAY_QR', 'PERSONAL_ALIPAY_PC', 'PERSONAL_ALIPAY_H5')
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `cashier_group_code` = 'EBANK',
    `cashier_group_name` = '网银支付',
    `cashier_group_sort` = 30,
    `updated_at` = NOW()
WHERE `method_code` IN ('PERSONAL_EBANK_REDIRECT', 'CORPORATE_EBANK_REDIRECT')
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `cashier_group_code` = 'OFFLINE_TRANSFER',
    `cashier_group_name` = '线下转账',
    `cashier_group_sort` = 40,
    `updated_at` = NOW()
WHERE `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `del_flag` = 0;

UPDATE `payment_method`
SET `cashier_group_code` = COALESCE(`instrument_type`, 'UNCLASSIFIED'),
    `cashier_group_name` = COALESCE(`method_name`, '未分类支付方式'),
    `cashier_group_sort` = 90,
    `updated_at` = NOW()
WHERE `cashier_group_code` IS NULL
   OR `cashier_group_name` IS NULL;

ALTER TABLE `payment_method`
  MODIFY COLUMN `cashier_group_code` varchar(64) NOT NULL COMMENT '收银台展示分组编码',
  MODIFY COLUMN `cashier_group_name` varchar(128) NOT NULL COMMENT '收银台展示分组名称';

CREATE INDEX `idx_payment_method_cashier_group`
  ON `payment_method` (`tenant_id`, `cashier_group_sort`, `sort`);
