ALTER TABLE `payment_method_route_rule`
  RENAME COLUMN `create_time` TO `created_at`,
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  RENAME COLUMN `update_time` TO `updated_at`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`,
  ADD COLUMN `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除' AFTER `updated_at`;

ALTER TABLE `payment_method_route_rule`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE `payment_method_route_rule_item`
  RENAME COLUMN `create_time` TO `created_at`,
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人ID' AFTER `tenant_id`,
  RENAME COLUMN `update_time` TO `updated_at`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人ID' AFTER `created_at`,
  ADD COLUMN `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除' AFTER `updated_at`;

ALTER TABLE `payment_method_route_rule_item`
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE `payment_method_route_rule`
  DROP INDEX `uk_payment_route_rule`,
  ADD UNIQUE KEY `uk_payment_route_rule` (`tenant_id`, `rule_code`, `del_flag`),
  ADD KEY `idx_payment_route_rule_match` (`tenant_id`, `app_id`, `subject_id`, `method_code`, `terminal_type`, `environment`, `status`, `del_flag`);

ALTER TABLE `payment_method_route_rule_item`
  DROP INDEX `uk_payment_route_rule_item`,
  ADD UNIQUE KEY `uk_payment_route_rule_item` (`tenant_id`, `rule_id`, `contract_capability_id`, `del_flag`),
  ADD KEY `idx_payment_route_item_rule` (`tenant_id`, `rule_id`, `status`, `del_flag`);
