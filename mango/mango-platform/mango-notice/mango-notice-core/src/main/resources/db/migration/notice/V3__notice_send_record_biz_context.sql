ALTER TABLE `notice_send_record`
  ADD COLUMN `biz_type` varchar(64) DEFAULT NULL COMMENT '业务类型' AFTER `recipient_id`,
  ADD COLUMN `biz_id` varchar(128) DEFAULT NULL COMMENT '业务对象 ID' AFTER `biz_type`;

CREATE INDEX `idx_notice_send_biz` ON `notice_send_record` (`tenant_id`, `biz_type`, `biz_id`);
