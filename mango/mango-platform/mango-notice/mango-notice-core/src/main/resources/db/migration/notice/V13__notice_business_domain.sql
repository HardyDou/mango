ALTER TABLE `notice_business_type`
  ADD COLUMN `domain_code` varchar(64) NOT NULL DEFAULT 'NOTICE' COMMENT '业务域编码' AFTER `biz_group`;

UPDATE `notice_business_type`
SET `domain_code` = CASE
  WHEN `biz_group` IS NULL OR `biz_group` = '' THEN 'NOTICE'
  ELSE `biz_group`
END
WHERE `domain_code` IS NULL OR `domain_code` = '';

CREATE INDEX `idx_notice_business_type_domain` ON `notice_business_type` (`tenant_id`, `domain_code`);
