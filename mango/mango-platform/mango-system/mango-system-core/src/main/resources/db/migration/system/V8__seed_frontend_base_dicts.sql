INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `status`, `remark`)
VALUES
(10, 'authorization_role_type', '授权角色类型', 1, '授权模块角色类型'),
(11, 'authorization_menu_type', '授权菜单类型', 1, '授权模块菜单节点类型'),
(12, 'system_param_type', '系统参数类型', 1, '系统参数管理类型'),
(13, 'system_config_type', '系统配置类型', 1, '系统配置分组类型'),
(14, 'system_route_type', '系统路由类型', 1, '系统路由资源类型'),
(15, 'sys_login_status', '登录状态', 1, '登录日志状态'),
(16, 'sys_operation_status', '操作状态', 1, '操作日志状态'),
(17, 'org_type', '组织类型', 1, '组织架构节点类型'),
(18, 'area_type', '行政区划类型', 1, '行政区划级别类型'),
(19, 'area_status', '行政区划状态', 1, '行政区划状态')
ON DUPLICATE KEY UPDATE
`dict_name` = VALUES(`dict_name`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`);

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`, `status`)
VALUES
(100, 'authorization_role_type', '系统角色', '1', 1, 1),
(101, 'authorization_role_type', '业务角色', '2', 2, 1),
(110, 'authorization_menu_type', '目录', '1', 1, 1),
(111, 'authorization_menu_type', '菜单', '2', 2, 1),
(112, 'authorization_menu_type', '按钮', '3', 3, 1),
(120, 'system_param_type', '系统参数', '1', 1, 1),
(121, 'system_param_type', '应用参数', '2', 2, 1),
(130, 'system_config_type', '系统', 'system', 1, 1),
(131, 'system_config_type', '业务', 'business', 2, 1),
(132, 'system_config_type', '安全', 'security', 3, 1),
(133, 'system_config_type', '功能', 'feature', 4, 1),
(134, 'system_config_type', '上传', 'upload', 5, 1),
(135, 'system_config_type', '邮件', 'email', 6, 1),
(136, 'system_config_type', '短信', 'sms', 7, 1),
(140, 'system_route_type', '菜单', '1', 1, 1),
(141, 'system_route_type', '按钮', '2', 2, 1),
(142, 'system_route_type', 'API', '3', 3, 1),
(150, 'sys_login_status', '成功', '1', 1, 1),
(151, 'sys_login_status', '失败', '0', 2, 1),
(160, 'sys_operation_status', '正常', '1', 1, 1),
(161, 'sys_operation_status', '异常', '0', 2, 1),
(170, 'org_type', '公司', 'company', 1, 1),
(171, 'org_type', '部门', 'department', 2, 1),
(172, 'org_type', '岗位', 'position', 3, 1),
(180, 'area_type', '国家', 'country', 1, 1),
(181, 'area_type', '省份', 'province', 2, 1),
(182, 'area_type', '城市', 'city', 3, 1),
(183, 'area_type', '区县', 'district', 4, 1),
(184, 'area_type', '街道', 'street', 5, 1),
(190, 'area_status', '未生效', '0', 1, 1),
(191, 'area_status', '生效', '1', 2, 1)
ON DUPLICATE KEY UPDATE
`dict_label` = VALUES(`dict_label`),
`dict_value` = VALUES(`dict_value`),
`sort` = VALUES(`sort`),
`status` = VALUES(`status`);
