ALTER TABLE `payment_refund_approval`
  ADD COLUMN `remark` varchar(512) DEFAULT NULL COMMENT '退款备注' AFTER `reason`;
