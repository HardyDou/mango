ALTER TABLE `workflow_definition`
  ADD COLUMN `admin_users` varchar(1000) DEFAULT NULL COMMENT '流程管理员用户名JSON数组' AFTER `group_id`;
