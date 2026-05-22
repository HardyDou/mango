ALTER TABLE `template`
  ADD COLUMN `draft_source_format` varchar(32) DEFAULT NULL COMMENT '未发布草稿源格式: TEXT HTML DOCX XLSX' AFTER `current_version_no`,
  ADD COLUMN `draft_content` longtext DEFAULT NULL COMMENT '未发布草稿文本或HTML内容' AFTER `draft_source_format`,
  ADD COLUMN `draft_source_file_id` bigint DEFAULT NULL COMMENT '未发布草稿文档模板源文件ID' AFTER `draft_content`,
  ADD COLUMN `draft_variable_schema` json DEFAULT NULL COMMENT '未发布草稿变量定义' AFTER `draft_source_file_id`,
  ADD COLUMN `has_unpublished_changes` tinyint NOT NULL DEFAULT '0' COMMENT '是否存在未发布变更: 0-否 1-是' AFTER `draft_variable_schema`;
