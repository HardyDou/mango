ALTER TABLE `payment_virtual_channel_payment`
  ADD UNIQUE KEY `uk_payment_virtual_pay_order` (`tenant_id`, `pay_order_no`);
