ALTER TABLE `payment_channel_contract_capability`
  MODIFY COLUMN `fee_rate` decimal(10,10) DEFAULT NULL COMMENT '费率，最多保留10位小数';
