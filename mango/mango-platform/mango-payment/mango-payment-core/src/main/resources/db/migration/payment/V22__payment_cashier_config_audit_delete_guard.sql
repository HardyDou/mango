ALTER TABLE `payment_cashier_config`
  RENAME COLUMN `create_by` TO `created_by`,
  RENAME COLUMN `update_by` TO `updated_by`,
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

UPDATE `payment_cashier_config`
SET `created_by` = NULL
WHERE `created_by` IS NOT NULL
  AND `created_by` NOT REGEXP '^[0-9]+$';

UPDATE `payment_cashier_config`
SET `updated_by` = NULL
WHERE `updated_by` IS NOT NULL
  AND `updated_by` NOT REGEXP '^[0-9]+$';

ALTER TABLE `payment_cashier_config`
  MODIFY COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  MODIFY COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE `payment_order`
  ADD COLUMN `cashier_config_id` bigint DEFAULT NULL COMMENT '收银台配置ID' AFTER `business_order_id`,
  ADD KEY `idx_payment_order_cashier` (`tenant_id`, `cashier_config_id`, `create_time`);
