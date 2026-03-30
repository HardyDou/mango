-- ============================================
-- Mango I18n Module Database Script
-- ============================================

-- Create sys_i18n table
CREATE TABLE IF NOT EXISTS `sys_i18n` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
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

INSERT INTO `sys_i18n` (`name`, `zh_cn`, `en`, `description`) VALUES
('common.save', '保存', 'Save', 'Save button'),
('common.cancel', '取消', 'Cancel', 'Cancel button'),
('common.delete', '删除', 'Delete', 'Delete button'),
('common.edit', '编辑', 'Edit', 'Edit button'),
('common.add', '新增', 'Add', 'Add button'),
('common.search', '搜索', 'Search', 'Search button'),
('common.reset', '重置', 'Reset', 'Reset button'),
('common.submit', '提交', 'Submit', 'Submit button'),
('common.confirm', '确认', 'Confirm', 'Confirm button'),
('common.back', '返回', 'Back', 'Back button'),
('menu.home', '首页', 'Home', 'Home menu'),
('menu.system', '系统管理', 'System', 'System management menu'),
('menu.user', '用户管理', 'User Management', 'User management menu'),
('menu.role', '角色管理', 'Role Management', 'Role management menu'),
('menu.permission', '权限管理', 'Permission Management', 'Permission management menu'),
('menu.org', '组织管理', 'Organization Management', 'Organization management menu'),
('menu.i18n', '国际化管理', 'I18n Management', 'I18n management menu'),
('message.success', '操作成功', 'Operation successful', 'Success message'),
('message.error', '操作失败', 'Operation failed', 'Error message'),
('message.unauthorized', '未授权访问', 'Unauthorized', 'Unauthorized message');
