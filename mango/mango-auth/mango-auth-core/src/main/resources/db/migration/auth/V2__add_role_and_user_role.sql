-- ================================================
-- V2: 角色 + 用户角色关联表（含 tenantId）
-- ================================================

-- 1. sys_permission 表（权限定义，代码注解为来源）
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键，雪花算法' PRIMARY KEY,
    `perm_code`   VARCHAR(100) NOT NULL COMMENT '权限码，格式 model:module:action，唯一' UNIQUE,
    `perm_name`   VARCHAR(50) NOT NULL COMMENT '权限名称（中文）',
    `perm_type`   VARCHAR(20) NOT NULL DEFAULT 'MENU' COMMENT '类型：MENU/BUTTON/API',
    `module`      VARCHAR(50) NOT NULL COMMENT '所属模块',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    KEY `idx_module` (`module`),
    KEY `idx_perm_type` (`perm_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限定义表';

-- 2. sys_role 表（含 tenantId）
CREATE TABLE IF NOT EXISTS `sys_role` (
    `role_id`     BIGINT(20) NOT NULL COMMENT '角色ID，雪花算法' PRIMARY KEY,
    `tenant_id`   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `role_code`   VARCHAR(100) NOT NULL COMMENT '角色标识(唯一)' UNIQUE,
    `role_name`   VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_type`   TINYINT NOT NULL DEFAULT 1 COMMENT '角色类型: 1-系统角色 2-业务角色',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `sort`        INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag`    TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 3. sys_user_role 表（含 tenantId，防止跨租户角色分配）
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键，雪花算法' PRIMARY KEY,
    `tenant_id`   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `role_id`     BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`, `tenant_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 4. sys_role_permission 表（角色-权限关联）
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键，雪花算法' PRIMARY KEY,
    `role_id`     BIGINT NOT NULL COMMENT '角色ID',
    `perm_id`     BIGINT NOT NULL COMMENT '权限ID（关联 sys_permission）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_perm_id` (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 5. sys_user_permission 表（用户直授权限，例外机制，不推荐常规使用）
CREATE TABLE IF NOT EXISTS `sys_user_permission` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键，雪花算法' PRIMARY KEY,
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `perm_id`     BIGINT NOT NULL COMMENT '权限ID（关联 sys_permission）',
    `tenant_id`   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_perm` (`user_id`, `perm_id`, `tenant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_perm_id` (`perm_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户直授权限表（例外机制）';

-- 6. 初始化超级管理员角色
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_code`, `role_name`, `role_type`, `status`, `sort`)
VALUES (1, 1, 'ROLE_ADMIN', '超级管理员', 1, 1, 0)
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

-- 7. 为 sys_menu 表增加 tenant_id 字段（如果不存在）
ALTER TABLE `sys_menu` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER `group_id`;

-- 8. 为 sys_role_menu 表增加 tenant_id 字段
ALTER TABLE `sys_role_menu` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER `id`;
