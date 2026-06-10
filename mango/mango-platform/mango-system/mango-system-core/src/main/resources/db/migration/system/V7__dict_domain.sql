ALTER TABLE `sys_dict_type`
  ADD COLUMN `domain_code` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '业务域编码' AFTER `dict_name`;

UPDATE `sys_dict_type`
SET `domain_code` = 'COMMON'
WHERE `domain_code` IS NULL OR `domain_code` = '';

CREATE INDEX `idx_sys_dict_type_domain` ON `sys_dict_type` (`tenant_id`, `domain_code`);
