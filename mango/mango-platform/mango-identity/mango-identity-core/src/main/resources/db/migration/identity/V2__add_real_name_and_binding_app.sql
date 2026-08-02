ALTER TABLE `identity_user`
  ADD COLUMN `real_name` varchar(100) DEFAULT NULL COMMENT '实名姓名' AFTER `avatar`,
  ADD COLUMN `document_type` varchar(32) DEFAULT NULL COMMENT '证件类型' AFTER `real_name`,
  ADD COLUMN `document_number` varchar(128) DEFAULT NULL COMMENT '证件号码' AFTER `document_type`,
  ADD COLUMN `verification_status` varchar(32) NOT NULL DEFAULT 'UNVERIFIED' COMMENT '实名认证状态' AFTER `document_number`,
  ADD COLUMN `verification_source` varchar(64) DEFAULT NULL COMMENT '实名认证来源' AFTER `verification_status`;

ALTER TABLE `identity_external_binding`
  ADD COLUMN `app_code` varchar(64) NOT NULL DEFAULT 'internal-admin' COMMENT '应用编码' AFTER `tenant_id`;

ALTER TABLE `identity_external_binding`
  DROP INDEX `uk_external_binding_external`,
  ADD UNIQUE KEY `uk_external_binding_external` (`tenant_id`, `app_code`, `provider`, `corp_id`, `external_user_id`),
  ADD KEY `idx_external_binding_user_app` (`tenant_id`, `app_code`, `user_id`);
