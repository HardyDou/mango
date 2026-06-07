ALTER TABLE `template`
  ADD COLUMN `domain_code` varchar(64) NOT NULL DEFAULT 'TEMPLATE' COMMENT '业务域编码' AFTER `category_name`;

UPDATE `template`
SET `domain_code` = CASE
  WHEN `category_code` IS NOT NULL AND `category_code` <> '' THEN `category_code`
  ELSE 'TEMPLATE'
END
WHERE `domain_code` IS NULL OR `domain_code` = '';

CREATE INDEX `idx_template_domain` ON `template` (`tenant_id`, `domain_code`);
