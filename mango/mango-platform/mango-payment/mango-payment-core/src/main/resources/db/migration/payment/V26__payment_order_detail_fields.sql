ALTER TABLE `payment_order`
  ADD COLUMN `channel_merchant_no` varchar(64) DEFAULT NULL COMMENT '通道商户号' AFTER `channel_id`,
  ADD COLUMN `contract_id` bigint DEFAULT NULL COMMENT '通道签约配置 ID' AFTER `channel_merchant_no`,
  ADD COLUMN `contract_capability_id` bigint DEFAULT NULL COMMENT '签约能力 ID' AFTER `contract_id`,
  ADD COLUMN `route_rule_id` bigint DEFAULT NULL COMMENT '路由规则 ID' AFTER `contract_capability_id`,
  ADD COLUMN `success_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否为业务订单有效成功支付：1-是，0-否' AFTER `channel_trade_no`,
  ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间' AFTER `success_flag`,
  ADD COLUMN `expire_time` datetime DEFAULT NULL COMMENT '支付订单过期时间' AFTER `pay_time`;

UPDATE `payment_order` `po`
LEFT JOIN `payment_channel_contract` `cc`
  ON `cc`.`tenant_id` = `po`.`tenant_id`
 AND `cc`.`channel_id` = `po`.`channel_id`
 AND `cc`.`status` = 1
 AND `cc`.`del_flag` = 0
SET `po`.`channel_merchant_no` = COALESCE(`po`.`channel_merchant_no`, `cc`.`merchant_no`),
    `po`.`contract_id` = COALESCE(`po`.`contract_id`, `cc`.`id`),
    `po`.`success_flag` = CASE WHEN `po`.`status` IN ('SUCCESS', 'PAID') THEN 1 ELSE 0 END,
    `po`.`pay_time` = CASE WHEN `po`.`status` IN ('SUCCESS', 'PAID') THEN COALESCE(`po`.`pay_time`, `po`.`update_time`) ELSE `po`.`pay_time` END,
    `po`.`expire_time` = COALESCE(`po`.`expire_time`, DATE_ADD(`po`.`create_time`, INTERVAL 30 MINUTE)),
    `po`.`status` = CASE WHEN `po`.`status` = 'PROCESSING' THEN 'PAYING' ELSE `po`.`status` END;

UPDATE `payment_order` `po`
JOIN `payment_method` `pm`
  ON `pm`.`tenant_id` = `po`.`tenant_id`
 AND `pm`.`id` = `po`.`method_id`
LEFT JOIN `payment_channel_contract_capability` `ccc`
  ON `ccc`.`tenant_id` = `po`.`tenant_id`
 AND `ccc`.`contract_id` = `po`.`contract_id`
 AND `ccc`.`method_code` = `pm`.`method_code`
 AND `ccc`.`status` = 1
 AND `ccc`.`del_flag` = 0
LEFT JOIN `payment_method_route_rule` `rr`
  ON `rr`.`tenant_id` = `po`.`tenant_id`
 AND `rr`.`method_code` = `pm`.`method_code`
 AND `rr`.`status` = 1
SET `po`.`contract_capability_id` = COALESCE(`po`.`contract_capability_id`, `ccc`.`id`),
    `po`.`route_rule_id` = COALESCE(`po`.`route_rule_id`, `rr`.`id`);

ALTER TABLE `payment_order`
  ADD KEY `idx_payment_order_contract` (`tenant_id`, `contract_id`, `contract_capability_id`),
  ADD KEY `idx_payment_order_success` (`tenant_id`, `business_order_id`, `success_flag`, `status`);
