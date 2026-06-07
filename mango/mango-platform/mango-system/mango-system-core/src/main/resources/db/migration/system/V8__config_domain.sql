ALTER TABLE `sys_config`
  ADD COLUMN `domain_code` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '业务域编码' AFTER `type`;

UPDATE `sys_config`
SET `domain_code` = 'COMMON'
WHERE `domain_code` IS NULL OR `domain_code` = '';

CREATE INDEX `idx_sys_config_domain` ON `sys_config` (`tenant_id`, `domain_code`);
