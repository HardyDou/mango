ALTER TABLE `numgen_generator`
  ADD COLUMN `domain_code` varchar(64) NOT NULL DEFAULT 'NUMGEN' COMMENT '业务域编码' AFTER `gen_name`;

UPDATE `numgen_generator`
SET `domain_code` = 'NUMGEN'
WHERE `domain_code` IS NULL OR `domain_code` = '';

CREATE INDEX `idx_numgen_generator_domain` ON `numgen_generator` (`tenant_id`, `domain_code`);
