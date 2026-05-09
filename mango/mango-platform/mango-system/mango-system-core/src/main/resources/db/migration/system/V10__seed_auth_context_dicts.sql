INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `status`, `remark`)
VALUES
(20, 'auth_realm', '认证登录域', 1, '认证授权上下文登录域'),
(21, 'auth_actor_type', '认证操作者类型', 1, '认证授权上下文操作者类型')
ON DUPLICATE KEY UPDATE
    `dict_name` = VALUES(`dict_name`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`, `status`)
VALUES
(200, 'auth_realm', '内部', 'INTERNAL', 1, 1),
(201, 'auth_realm', '租户', 'TENANT', 2, 1),
(202, 'auth_realm', '客户', 'CUSTOMER', 3, 1),
(203, 'auth_realm', '合作方', 'PARTNER', 4, 1),
(204, 'auth_realm', '金融机构', 'FINANCIAL', 5, 1),
(210, 'auth_actor_type', '内部用户', 'INTERNAL_USER', 1, 1),
(211, 'auth_actor_type', '租户用户', 'TENANT_USER', 2, 1),
(212, 'auth_actor_type', '客户用户', 'CUSTOMER_USER', 3, 1),
(213, 'auth_actor_type', '合作方用户', 'PARTNER_USER', 4, 1),
(214, 'auth_actor_type', '金融机构用户', 'FINANCIAL_USER', 5, 1)
ON DUPLICATE KEY UPDATE
    `dict_label` = VALUES(`dict_label`),
    `dict_value` = VALUES(`dict_value`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`);
