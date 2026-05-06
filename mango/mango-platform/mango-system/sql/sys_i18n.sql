-- ============================================
-- Mango I18n Module Database Script
-- ============================================

-- Create sys_i18n table
CREATE TABLE IF NOT EXISTS `sys_i18n` (
  `id` BIGINT(20) NOT NULL COMMENT 'Primary key',
  `name` VARCHAR(100) NOT NULL COMMENT 'i18n key',
  `zh_cn` VARCHAR(500) DEFAULT NULL COMMENT 'Chinese content',
  `en` VARCHAR(500) DEFAULT NULL COMMENT 'English content',
  `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Internationalization table';

-- ============================================
-- Sample Data
-- ============================================

INSERT INTO `sys_i18n` (`id`, `name`, `zh_cn`, `en`, `description`) VALUES
(1, 'common.save', '保存', 'Save', 'Save button'),
(2, 'common.cancel', '取消', 'Cancel', 'Cancel button'),
(3, 'common.delete', '删除', 'Delete', 'Delete button'),
(4, 'common.edit', '编辑', 'Edit', 'Edit button'),
(5, 'common.add', '新增', 'Add', 'Add button'),
(6, 'common.search', '搜索', 'Search', 'Search button'),
(7, 'common.reset', '重置', 'Reset', 'Reset button'),
(8, 'common.submit', '提交', 'Submit', 'Submit button'),
(9, 'common.confirm', '确认', 'Confirm', 'Confirm button'),
(10, 'common.back', '返回', 'Back', 'Back button'),
(11, 'menu.home', '首页', 'Home', 'Home menu'),
(12, 'menu.system', '系统管理', 'System', 'System management menu'),
(13, 'menu.user', '用户管理', 'User Management', 'User management menu'),
(14, 'menu.role', '角色管理', 'Role Management', 'Role management menu'),
(15, 'menu.permission', '权限管理', 'Permission Management', 'Permission management menu'),
(16, 'menu.org', '组织管理', 'Organization Management', 'Organization management menu'),
(17, 'menu.i18n', '国际化管理', 'I18n Management', 'I18n management menu'),
(18, 'message.success', '操作成功', 'Operation successful', 'Success message'),
(19, 'message.error', '操作失败', 'Operation failed', 'Error message'),
(20, 'message.unauthorized', '未授权访问', 'Unauthorized', 'Unauthorized message');
