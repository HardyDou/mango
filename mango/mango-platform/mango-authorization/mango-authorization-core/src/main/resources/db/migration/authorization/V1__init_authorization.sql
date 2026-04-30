-- ================================================
-- V1: 授权模块初始化表结构
-- ================================================

CREATE TABLE IF NOT EXISTS `authorization_api_resource` (
    `id` BIGINT NOT NULL COMMENT '主键' PRIMARY KEY,
    `module_name` VARCHAR(128) NOT NULL COMMENT '稳定模块名',
    `http_method` VARCHAR(16) NOT NULL COMMENT 'HTTP 方法',
    `path_pattern` VARCHAR(512) NOT NULL COMMENT '接口路径模式',
    `resource_code` VARCHAR(640) NOT NULL COMMENT '资源编码',
    `permission_code` VARCHAR(255) DEFAULT NULL COMMENT '权限编码',
    `access_mode` VARCHAR(32) NOT NULL DEFAULT 'LOGIN' COMMENT '访问模式: PUBLIC/LOGIN/PERMISSION/INTERNAL',
    `handler_class` VARCHAR(512) DEFAULT NULL COMMENT '处理类',
    `handler_method` VARCHAR(128) DEFAULT NULL COMMENT '处理方法',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-正常, 1-已删除',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
    UNIQUE KEY `uk_module_method_path` (`module_name`, `http_method`, `path_pattern`),
    KEY `idx_module_name` (`module_name`),
    KEY `idx_resource_code` (`resource_code`),
    KEY `idx_permission_code` (`permission_code`),
    KEY `idx_access_mode` (`access_mode`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口资源表';

CREATE TABLE IF NOT EXISTS `authorization_permission` (
    `id` BIGINT NOT NULL COMMENT '主键' PRIMARY KEY,
    `perm_code` VARCHAR(100) NOT NULL COMMENT '权限码，格式 model:module:action' UNIQUE,
    `perm_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
    `perm_type` VARCHAR(20) NOT NULL DEFAULT 'MENU' COMMENT '类型：MENU/BUTTON/API',
    `module` VARCHAR(50) NOT NULL COMMENT '所属模块',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
    KEY `idx_module` (`module`),
    KEY `idx_perm_type` (`perm_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限定义表';

CREATE TABLE IF NOT EXISTS `authorization_role` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID' PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `app_code` VARCHAR(64) NOT NULL DEFAULT 'internal-admin' COMMENT '应用编码',
    `realm` VARCHAR(32) NOT NULL DEFAULT 'INTERNAL' COMMENT '登录域',
    `actor_type` VARCHAR(32) DEFAULT NULL COMMENT '操作者类型',
    `role_code` VARCHAR(100) NOT NULL COMMENT '角色标识',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_type` TINYINT NOT NULL DEFAULT 1 COMMENT '角色类型: 1-系统角色 2-业务角色',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_app_role_code` (`app_code`, `role_code`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_app_code` (`app_code`),
    KEY `idx_realm_actor` (`realm`, `actor_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `authorization_subject_role` (
    `id` BIGINT NOT NULL COMMENT '主键' PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `subject_id` BIGINT NOT NULL COMMENT '主体ID',
    `app_code` VARCHAR(64) DEFAULT NULL COMMENT '应用编码',
    `realm` VARCHAR(32) DEFAULT NULL COMMENT '登录域',
    `actor_type` VARCHAR(32) DEFAULT NULL COMMENT '操作者类型',
    `party_type` VARCHAR(64) DEFAULT NULL COMMENT '归属主体类型',
    `party_id` BIGINT DEFAULT NULL COMMENT '归属主体ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_subject_role` (`subject_id`, `role_id`, `tenant_id`, `app_code`, `party_type`, `party_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_subject_context` (`subject_id`, `app_code`, `realm`, `party_type`, `party_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体角色关联表';

CREATE TABLE IF NOT EXISTS `authorization_role_permission` (
    `id` BIGINT NOT NULL COMMENT '主键' PRIMARY KEY,
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `perm_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
    UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_perm_id` (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS `authorization_subject_permission` (
    `id` BIGINT NOT NULL COMMENT '主键' PRIMARY KEY,
    `subject_id` BIGINT NOT NULL COMMENT '主体ID',
    `perm_id` BIGINT NOT NULL COMMENT '权限ID',
    `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_subject_perm` (`subject_id`, `perm_id`, `tenant_id`),
    KEY `idx_subject_id` (`subject_id`),
    KEY `idx_perm_id` (`perm_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体直授权限表';

CREATE TABLE IF NOT EXISTS `authorization_app` (
    `app_id` BIGINT NOT NULL COMMENT '应用ID' PRIMARY KEY,
    `app_code` VARCHAR(64) NOT NULL COMMENT '应用编码',
    `app_name` VARCHAR(100) NOT NULL COMMENT '应用名称',
    `realm` VARCHAR(32) NOT NULL COMMENT '登录域',
    `actor_type` VARCHAR(32) DEFAULT NULL COMMENT '默认操作者类型',
    `icon` VARCHAR(64) DEFAULT NULL COMMENT '应用图标',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
    UNIQUE KEY `uk_authorization_app_code` (`app_code`),
    KEY `idx_authorization_app_realm` (`realm`),
    KEY `idx_authorization_app_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='授权应用入口表';

CREATE TABLE IF NOT EXISTS `authorization_menu` (
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID' PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `app_code` VARCHAR(64) NOT NULL DEFAULT 'internal-admin' COMMENT '应用编码',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    `menu_type` TINYINT NOT NULL DEFAULT 1 COMMENT '菜单类型: 1-目录, 2-菜单, 3-按钮',
    `menu_name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
    `menu_code` VARCHAR(128) DEFAULT NULL COMMENT '菜单权限标识',
    `path` VARCHAR(255) DEFAULT NULL COMMENT '前端路由路径',
    `icon` VARCHAR(64) DEFAULT NULL COMMENT '菜单图标',
    `component` VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示: 0-隐藏, 1-显示',
    `keep_alive` TINYINT NOT NULL DEFAULT 0 COMMENT '路由缓存: 0-不缓存, 1-缓存',
    `embedded` TINYINT NOT NULL DEFAULT 0 COMMENT '内嵌模式: 0-否, 1-iframe内嵌',
    `redirect` VARCHAR(255) DEFAULT NULL COMMENT '重定向路径',
    `permissions` VARCHAR(500) DEFAULT NULL COMMENT '权限标识列表',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '修改人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-正常, 1-已删除',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_app_parent` (`app_code`, `parent_id`),
    KEY `idx_menu_type` (`menu_type`),
    KEY `idx_status` (`status`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS `authorization_role_menu` (
    `id` BIGINT NOT NULL COMMENT '主键' PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

INSERT INTO `authorization_app` (`app_id`, `app_code`, `app_name`, `realm`, `actor_type`, `icon`, `sort`, `status`) VALUES
(1, 'internal-admin', '内部管理后台', 'INTERNAL', 'INTERNAL_USER', 'Setting', 1, 1)
ON DUPLICATE KEY UPDATE `app_name` = VALUES(`app_name`);

INSERT INTO `authorization_role` (`role_id`, `tenant_id`, `app_code`, `realm`, `actor_type`, `role_code`, `role_name`, `role_type`, `status`, `sort`)
VALUES (1, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'ROLE_ADMIN', '超级管理员', 1, 1, 0)
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

INSERT INTO `authorization_menu` (`menu_id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`, `keep_alive`, `embedded`) VALUES
(1, 1, 'internal-admin', 0, 1, '系统管理', 'system', '/system', NULL, 'Setting', 1, 1, 1, 0, 0),
(2, 1, 'internal-admin', 1, 2, '用户管理', 'system:user', '/system/user', '@/views/system/user/index.vue', 'User', 1, 1, 1, 0, 0),
(3, 1, 'internal-admin', 1, 2, '角色管理', 'system:role', '/system/role', '@/views/system/role/index.vue', 'Role', 2, 1, 1, 0, 0),
(4, 1, 'internal-admin', 1, 2, '菜单管理', 'system:menu', '/system/menu', '@/views/system/menu/index.vue', 'Menu', 3, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`);
