ALTER TABLE `payment_channel_contract`
  RENAME COLUMN `create_by` TO `created_by`,
  RENAME COLUMN `update_by` TO `updated_by`,
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

UPDATE `payment_channel_contract`
SET `created_by` = NULL
WHERE `created_by` IS NOT NULL
  AND `created_by` NOT REGEXP '^[0-9]+$';

UPDATE `payment_channel_contract`
SET `updated_by` = NULL
WHERE `updated_by` IS NOT NULL
  AND `updated_by` NOT REGEXP '^[0-9]+$';

ALTER TABLE `payment_channel_contract`
  MODIFY COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  MODIFY COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE `payment_channel_contract_capability`
  ADD COLUMN `certificate_expire_time` datetime DEFAULT NULL COMMENT '证书有效期' AFTER `priority`,
  ADD COLUMN `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除' AFTER `tenant_id`,
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `del_flag`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_by`,
  RENAME COLUMN `create_time` TO `created_at`,
  RENAME COLUMN `update_time` TO `updated_at`;

ALTER TABLE `payment_channel_contract_capability`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  DROP INDEX `uk_payment_contract_capability`,
  ADD UNIQUE KEY `uk_payment_contract_capability` (`tenant_id`, `contract_id`, `channel_capability_id`, `del_flag`);
