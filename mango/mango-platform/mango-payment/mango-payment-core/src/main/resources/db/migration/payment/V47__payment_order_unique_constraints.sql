ALTER TABLE `payment_order`
  ADD COLUMN `channel_code` varchar(64) DEFAULT NULL COMMENT '支付通道编码' AFTER `channel_id`;

UPDATE `payment_order` `po`
JOIN `payment_channel` `pc`
  ON `pc`.`tenant_id` = `po`.`tenant_id`
 AND `pc`.`id` = `po`.`channel_id`
SET `po`.`channel_code` = `pc`.`channel_code`
WHERE `po`.`channel_code` IS NULL;

ALTER TABLE `payment_order`
  MODIFY COLUMN `channel_code` varchar(64) NOT NULL COMMENT '支付通道编码',
  ADD COLUMN `success_business_order_id` bigint
    GENERATED ALWAYS AS (CASE WHEN `success_flag` = 1 THEN `business_order_id` ELSE NULL END) STORED
    COMMENT '有效成功支付唯一约束辅助列',
  ADD UNIQUE KEY `uk_payment_order_channel_trade` (`tenant_id`, `channel_code`, `channel_trade_no`),
  ADD UNIQUE KEY `uk_payment_order_success_business` (`tenant_id`, `success_business_order_id`);
