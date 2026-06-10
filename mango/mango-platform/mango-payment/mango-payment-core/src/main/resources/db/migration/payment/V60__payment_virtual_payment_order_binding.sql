ALTER TABLE `payment_virtual_channel_payment`
  ADD COLUMN `pay_order_no` varchar(64) DEFAULT NULL COMMENT '支付订单号' AFTER `virtual_payment_no`,
  ADD COLUMN `channel_trade_no` varchar(128) DEFAULT NULL COMMENT '通道交易号' AFTER `pay_order_no`,
  ADD KEY `idx_payment_virtual_pay_order` (`tenant_id`, `pay_order_no`),
  ADD UNIQUE KEY `uk_payment_virtual_channel_trade` (`tenant_id`, `channel_trade_no`);
