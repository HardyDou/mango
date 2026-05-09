ALTER TABLE `authorization_role`
    DROP INDEX `uk_authorization_role_app_role_code`,
    ADD UNIQUE KEY `uk_authorization_role_tenant_app_role_code` (`tenant_id`, `app_code`, `role_code`);

INSERT INTO `authorization_role` (
    `id`, `tenant_id`, `app_code`, `realm`, `actor_type`, `role_code`, `role_name`,
    `role_type`, `status`, `sort`, `remark`
) VALUES
    (1, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'ROLE_ADMIN', '超级管理员', 1, 1, 1, '芒果集团管理员'),
    (2, 2, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'ROLE_ADMIN', '超级管理员', 1, 1, 1, 'A公司管理员'),
    (3, 3, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'ROLE_ADMIN', '超级管理员', 1, 1, 1, 'B公司管理员'),
    (4, 4, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'ROLE_ADMIN', '超级管理员', 1, 1, 1, 'C公司管理员')
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `app_code` = VALUES(`app_code`),
    `realm` = VALUES(`realm`),
    `actor_type` = VALUES(`actor_type`),
    `role_code` = VALUES(`role_code`),
    `role_name` = VALUES(`role_name`),
    `role_type` = VALUES(`role_type`),
    `status` = VALUES(`status`),
    `sort` = VALUES(`sort`),
    `remark` = VALUES(`remark`);

INSERT INTO `authorization_subject_role` (
    `id`, `tenant_id`, `subject_id`, `app_code`, `realm`, `actor_type`,
    `party_type`, `party_id`, `role_id`
) VALUES
    (1, 1, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'INTERNAL_ORG', 1, 1),
    (2, 2, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'INTERNAL_ORG', 1, 2),
    (3, 3, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'INTERNAL_ORG', 1, 3),
    (4, 4, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'INTERNAL_ORG', 1, 4)
ON DUPLICATE KEY UPDATE
    `tenant_id` = VALUES(`tenant_id`),
    `subject_id` = VALUES(`subject_id`),
    `app_code` = VALUES(`app_code`),
    `realm` = VALUES(`realm`),
    `actor_type` = VALUES(`actor_type`),
    `party_type` = VALUES(`party_type`),
    `party_id` = VALUES(`party_id`),
    `role_id` = VALUES(`role_id`);
