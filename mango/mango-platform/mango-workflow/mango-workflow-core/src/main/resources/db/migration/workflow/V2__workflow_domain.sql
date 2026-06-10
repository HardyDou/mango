ALTER TABLE `workflow_category`
  ADD COLUMN `domain_code` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '业务域编码' AFTER `category_code`;

UPDATE `workflow_category`
SET `domain_code` = 'COMMON'
WHERE `domain_code` IS NULL OR `domain_code` = '';

CREATE INDEX `idx_workflow_category_domain` ON `workflow_category` (`tenant_id`, `domain_code`);

ALTER TABLE `workflow_definition`
  ADD COLUMN `domain_code` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '业务域编码' AFTER `category_id`;

UPDATE `workflow_definition` d
LEFT JOIN `workflow_category` c ON c.`id` = d.`category_id`
SET d.`domain_code` = COALESCE(NULLIF(c.`domain_code`, ''), 'COMMON')
WHERE d.`domain_code` IS NULL OR d.`domain_code` = '';

CREATE INDEX `idx_workflow_definition_domain` ON `workflow_definition` (`tenant_id`, `domain_code`);

ALTER TABLE `workflow_definition_version`
  ADD COLUMN `domain_code` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '业务域编码' AFTER `category_id`;

UPDATE `workflow_definition_version` v
LEFT JOIN `workflow_definition` d ON d.`id` = v.`definition_id`
SET v.`domain_code` = COALESCE(NULLIF(d.`domain_code`, ''), 'COMMON')
WHERE v.`domain_code` IS NULL OR v.`domain_code` = '';

CREATE INDEX `idx_workflow_definition_version_domain` ON `workflow_definition_version` (`tenant_id`, `domain_code`);
