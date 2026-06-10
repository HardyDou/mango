ALTER TABLE `payment_application`
  ADD COLUMN `app_id` varchar(64) DEFAULT NULL COMMENT 'AppId，业务系统调用支付平台的应用身份' AFTER `id`;

UPDATE `payment_application`
SET `app_id` = CASE
  WHEN `id` = 310001 THEN 'app_order_center'
  WHEN `id` = 310002 THEN 'app_member_center'
  ELSE CONCAT('app_', LOWER(REPLACE(`app_code`, '_', '-')))
END
WHERE `app_id` IS NULL OR `app_id` = '';

ALTER TABLE `payment_application`
  MODIFY COLUMN `app_id` varchar(64) NOT NULL COMMENT 'AppId，业务系统调用支付平台的应用身份',
  DROP INDEX `uk_payment_application_tenant_code`,
  ADD UNIQUE KEY `uk_payment_application_tenant_app_id` (`tenant_id`, `app_id`, `del_flag`),
  DROP COLUMN `app_code`;

ALTER TABLE `payment_cashier_config`
  DROP COLUMN `cashier_code`;
