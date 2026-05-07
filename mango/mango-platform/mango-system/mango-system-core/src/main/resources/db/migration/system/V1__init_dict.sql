CREATE TABLE IF NOT EXISTS `sys_dict_type` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
    `dict_type`   VARCHAR(50) NOT NULL COMMENT '字典类型' UNIQUE,
    `dict_name`   VARCHAR(100) NOT NULL COMMENT '字典名称',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
    KEY `idx_sys_dict_type_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

CREATE TABLE IF NOT EXISTS `sys_dict_data` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
    `dict_type`   VARCHAR(50) NOT NULL COMMENT '字典类型',
    `dict_label`  VARCHAR(100) NOT NULL COMMENT '字典标签',
    `dict_value`  VARCHAR(100) NOT NULL COMMENT '字典值',
    `sort`        INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
    KEY `idx_sys_dict_data_type` (`dict_type`),
    KEY `idx_sys_dict_data_value` (`dict_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `status`) VALUES
(1, 'sys_user_sex', '用户性别', 1),
(2, 'sys_normal_disable', '系统开关', 1),
(3, 'sys_login_type', '登录类型', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`, `status`) VALUES
(1, 'sys_user_sex', '男', '0', 1, 1),
(2, 'sys_user_sex', '女', '1', 2, 1),
(3, 'sys_normal_disable', '启用', '1', 1, 1),
(4, 'sys_normal_disable', '禁用', '0', 2, 1),
(5, 'sys_login_type', '账号密码', 'username', 1, 1),
(6, 'sys_login_type', '手机号', 'mobile', 2, 1);
