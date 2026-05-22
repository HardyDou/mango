ALTER TABLE `template`
  ADD COLUMN `business_group` varchar(64) DEFAULT NULL COMMENT '业务组编码' AFTER `category_name`,
  ADD COLUMN `business_type` varchar(64) DEFAULT NULL COMMENT '业务类型' AFTER `business_group`,
  ADD COLUMN `business_key` varchar(128) DEFAULT NULL COMMENT '业务Key' AFTER `business_type`;

CREATE INDEX `idx_template_business_scope`
  ON `template` (`tenant_id`, `business_group`, `business_type`, `business_key`);
