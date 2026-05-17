ALTER TABLE `workflow_definition`
  ADD COLUMN `icon` varchar(64) DEFAULT NULL COMMENT '流程图标' AFTER `admin_users`;
