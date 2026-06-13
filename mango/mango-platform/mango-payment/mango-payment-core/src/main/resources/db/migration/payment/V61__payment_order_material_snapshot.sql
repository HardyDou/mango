ALTER TABLE `payment_order`
  ADD COLUMN `payment_material_json` json DEFAULT NULL COMMENT '通道支付物料快照' AFTER `channel_trade_no`;
