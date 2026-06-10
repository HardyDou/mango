ALTER TABLE `payment_operation_audit`
  ADD COLUMN `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除' AFTER `updated_at`;
