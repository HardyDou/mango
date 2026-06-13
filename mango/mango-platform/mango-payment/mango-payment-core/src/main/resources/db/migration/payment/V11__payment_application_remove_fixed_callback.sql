ALTER TABLE `payment_application`
  DROP COLUMN `notify_url`,
  DROP COLUMN `refund_notify_url`,
  DROP COLUMN `notify_url_whitelist`,
  DROP COLUMN `return_domain_whitelist`,
  DROP COLUMN `return_url`;

ALTER TABLE `payment_business_order`
  ADD COLUMN `notify_url` varchar(512) DEFAULT NULL COMMENT '本订单支付事件通知地址' AFTER `expire_time`,
  ADD COLUMN `return_url` varchar(512) DEFAULT NULL COMMENT '本订单支付完成返回地址' AFTER `notify_url`;
