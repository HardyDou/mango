-- 菜单权限表初始化
-- V1: 初始版本，包含完整的菜单权限字段

CREATE TABLE IF NOT EXISTS `perm_menu_group` (
    `group_id` BIGINT NOT NULL COMMENT '分组ID' PRIMARY KEY,
    `group_name` VARCHAR(64) NOT NULL COMMENT '分组名称',
    `group_code` VARCHAR(64) NOT NULL COMMENT '分组编码',
    `icon` VARCHAR(64) DEFAULT NULL COMMENT '分组图标',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '修改人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-正常, 1-已删除',
    KEY `idx_status` (`status`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单分组表';

CREATE TABLE IF NOT EXISTS `sys_menu` (
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID' PRIMARY KEY,
    `group_id` BIGINT DEFAULT NULL COMMENT '菜单分组ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID (0:根节点)',
    `menu_type` TINYINT NOT NULL DEFAULT 1 COMMENT '菜单类型: 1-目录, 2-菜单, 3-按钮',
    `menu_name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
    `menu_code` VARCHAR(128) DEFAULT NULL COMMENT '菜单权限标识 (如: system:user:view)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT '前端路由路径',
    `icon` VARCHAR(64) DEFAULT NULL COMMENT '菜单图标',
    `component` VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径 (如: @/views/system/user/index.vue)',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示: 0-隐藏, 1-显示',
    `keep_alive` TINYINT NOT NULL DEFAULT 0 COMMENT '路由缓存: 0-不缓存, 1-缓存',
    `embedded` TINYINT NOT NULL DEFAULT 0 COMMENT '内嵌模式: 0-否, 1-iframe内嵌',
    `redirect` VARCHAR(255) DEFAULT NULL COMMENT '重定向路径',
    `permissions` VARCHAR(500) DEFAULT NULL COMMENT '权限标识列表 (如: system:user:add,system:user:edit)',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '修改人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-正常, 1-已删除',
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_menu_type` (`menu_type`),
    KEY `idx_status` (`status`),
    KEY `idx_del_flag` (`del_flag`),
    KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- 初始化示例菜单分组数据
INSERT INTO `perm_menu_group` (`group_id`, `group_name`, `group_code`, `icon`, `sort`, `status`) VALUES
(1, '系统管理', 'system', 'Setting', 1, 1),
(2, '运维管理', 'ops', 'Monitor', 2, 1),
(3, '业务管理', 'biz', 'Folder', 3, 1);

-- 初始化示例菜单数据
INSERT INTO `sys_menu` (`menu_id`, `group_id`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `component`, `icon`, `sort`, `status`, `visible`, `keep_alive`, `embedded`) VALUES
(1, 1, 0, 1, '系统管理', 'system', '/system', NULL, 'Setting', 1, 1, 1, 0, 0),
(2, 1, 1, 2, '用户管理', 'system:user', '/system/user', '@/views/system/user/index.vue', 'User', 1, 1, 1, 0, 0),
(3, 1, 1, 2, '角色管理', 'system:role', '/system/role', '@/views/system/role/index.vue', 'Role', 2, 1, 1, 0, 0),
(4, 1, 1, 2, '菜单管理', 'system:menu', '/system/menu', '@/views/system/menu/index.vue', 'Menu', 3, 1, 1, 0, 0);

-- 菜单与角色关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `id` BIGINT NOT NULL COMMENT '主键' PRIMARY KEY,
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';
