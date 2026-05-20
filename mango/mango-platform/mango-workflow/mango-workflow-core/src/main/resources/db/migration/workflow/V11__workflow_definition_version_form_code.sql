ALTER TABLE `workflow_definition_version`
  ADD COLUMN `category_id` bigint DEFAULT NULL COMMENT '发布时流程分类ID快照' AFTER `version_no`,
  ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT '发布时所属组织ID快照' AFTER `category_id`,
  ADD COLUMN `admin_users` varchar(1000) DEFAULT NULL COMMENT '发布时流程管理员用户名JSON数组快照' AFTER `org_id`,
  ADD COLUMN `icon` varchar(512) DEFAULT NULL COMMENT '发布时流程图标快照' AFTER `admin_users`,
  ADD COLUMN `definition_name` varchar(128) DEFAULT NULL COMMENT '发布时流程名称快照' AFTER `icon`,
  ADD COLUMN `definition_key` varchar(128) DEFAULT NULL COMMENT '发布时流程编码快照' AFTER `definition_name`,
  ADD COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '发布时备注快照' AFTER `definition_key`,
  ADD COLUMN `form_code` varchar(128) DEFAULT NULL COMMENT '发布时表单编码快照' AFTER `remark`;
