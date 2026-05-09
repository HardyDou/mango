DELETE FROM `sys_tenant`
WHERE `tenant_code` IN ('test', 'demo', 'internal', 'tenant_a')
   OR `tenant_code` LIKE 'debug_tenant_%'
   OR `tenant_code` LIKE 'page_tenant_%'
   OR `tenant_code` LIKE 'page_verify_%'
   OR `tenant_code` LIKE 'curl_tenant_%'
   OR `tenant_code` LIKE 'api_tenant_%'
   OR `tenant_code` LIKE 'intercept_tenant_%';

INSERT INTO `sys_tenant`
    (`id`, `tenant_name`, `tenant_code`, `status`, `contact`, `mobile`, `email`, `remark`, `tenant_id`)
VALUES
    (1, '芒果集团', 'default', 1, '平台管理员', '13800000000', 'admin@mango.com', '平台默认租户', 'default'),
    (2, 'A公司', 'company_a', 1, 'A公司管理员', '13800000001', 'admin@company-a.com', 'A公司租户', 'default'),
    (3, 'B公司', 'company_b', 1, 'B公司管理员', '13800000002', 'admin@company-b.com', 'B公司租户', 'default'),
    (4, 'C公司', 'company_c', 1, 'C公司管理员', '13800000003', 'admin@company-c.com', 'C公司租户', 'default')
ON DUPLICATE KEY UPDATE
    `tenant_name` = VALUES(`tenant_name`),
    `status` = VALUES(`status`),
    `contact` = VALUES(`contact`),
    `mobile` = VALUES(`mobile`),
    `email` = VALUES(`email`),
    `remark` = VALUES(`remark`),
    `tenant_id` = VALUES(`tenant_id`);
