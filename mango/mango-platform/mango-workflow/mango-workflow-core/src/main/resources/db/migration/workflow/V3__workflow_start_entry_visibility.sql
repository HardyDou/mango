ALTER TABLE `workflow_definition`
  ADD COLUMN `start_entry_visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '启动入口是否可见: 0-隐藏 1-可见' AFTER `admin_users`;

CREATE INDEX `idx_workflow_definition_start_entry` ON `workflow_definition` (`tenant_id`, `start_entry_visible`);

ALTER TABLE `workflow_definition_version`
  ADD COLUMN `start_entry_visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '发布时启动入口是否可见快照: 0-隐藏 1-可见' AFTER `admin_users`;
