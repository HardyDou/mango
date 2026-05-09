ALTER TABLE `authorization_subject_role`
    ADD COLUMN `subject_type` VARCHAR(32) NOT NULL DEFAULT 'TENANT_MEMBER' COMMENT '主体类型' AFTER `subject_id`;

UPDATE `authorization_subject_role` `sr`
JOIN `tenant_member` `tm`
  ON `tm`.`tenant_id` = `sr`.`tenant_id`
 AND `tm`.`user_id` = `sr`.`subject_id`
SET `sr`.`subject_id` = `tm`.`id`,
    `sr`.`subject_type` = 'TENANT_MEMBER';

ALTER TABLE `authorization_subject_role`
    DROP INDEX `uk_authorization_subject_role_subject_role`,
    DROP INDEX `idx_authorization_subject_role_context`,
    ADD UNIQUE KEY `uk_authorization_subject_role_subject_role`
        (`subject_type`, `subject_id`, `role_id`, `tenant_id`, `app_code`, `party_type`, `party_id`),
    ADD KEY `idx_authorization_subject_role_context`
        (`subject_type`, `subject_id`, `app_code`, `realm`, `party_type`, `party_id`);
