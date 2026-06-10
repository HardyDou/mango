ALTER TABLE `payment_offline_collection`
  ADD COLUMN `transfer_amount` bigint DEFAULT NULL COMMENT '用户提交转账金额，单位分' AFTER `currency`,
  ADD COLUMN `voucher_file_ids` varchar(512) DEFAULT NULL COMMENT '转账凭证文件ID，多个用英文逗号分隔' AFTER `transfer_amount`,
  ADD COLUMN `submitted_time` datetime DEFAULT NULL COMMENT '用户提交凭证时间' AFTER `voucher_file_ids`,
  ADD COLUMN `submit_remark` varchar(512) DEFAULT NULL COMMENT '用户提交说明' AFTER `submitted_time`,
  ADD COLUMN `confirmed_amount` bigint DEFAULT NULL COMMENT '确认到账金额，单位分' AFTER `submit_remark`,
  ADD COLUMN `confirmed_by` bigint DEFAULT NULL COMMENT '确认人ID' AFTER `confirmed_time`,
  ADD COLUMN `confirmed_by_name` varchar(128) DEFAULT NULL COMMENT '确认人名称' AFTER `confirmed_by`,
  ADD COLUMN `confirm_remark` varchar(512) DEFAULT NULL COMMENT '确认说明' AFTER `confirmed_by_name`;
