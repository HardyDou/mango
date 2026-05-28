-- Baseline migration for module: template
-- Squashed from 5 migration files before first shared release.

-- -----------------------------------------------------------------------------
-- Squashed from: V1__init_template.sql
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `template` (
  `id` bigint NOT NULL COMMENT '模板ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `template_code` varchar(128) NOT NULL COMMENT '模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `category_code` varchar(64) DEFAULT NULL COMMENT '分类编码',
  `category_name` varchar(64) DEFAULT NULL COMMENT '分类名称',
  `source_format` varchar(32) DEFAULT NULL COMMENT '当前发布内容稿源格式: TEXT HTML DOCX XLSX',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用 1-启用',
  `current_version_no` int NOT NULL DEFAULT '0' COMMENT '当前发布版本号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_tenant_code` (`tenant_id`,`template_code`),
  KEY `idx_template_category` (`tenant_id`,`category_code`),
  KEY `idx_template_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板表';

CREATE TABLE IF NOT EXISTS `template_category` (
  `id` bigint NOT NULL COMMENT '模板分类ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `category_name` varchar(128) NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用 1-启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_category_tenant_code` (`tenant_id`,`category_code`),
  KEY `idx_template_category_status` (`tenant_id`,`status`),
  KEY `idx_template_category_sort` (`tenant_id`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板分类表';

CREATE TABLE IF NOT EXISTS `template_version` (
  `id` bigint NOT NULL COMMENT '模板版本ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `version_no` int NOT NULL COMMENT '版本号',
  `source_format` varchar(32) NOT NULL COMMENT '内容稿源格式: TEXT HTML DOCX XLSX',
  `content` longtext DEFAULT NULL COMMENT '文本或HTML模板内容',
  `source_file_id` bigint DEFAULT NULL COMMENT '文档模板源文件ID',
  `variable_schema` json DEFAULT NULL COMMENT '变量定义',
  `current_published` tinyint NOT NULL DEFAULT '0' COMMENT '是否当前发布版本: 0-否 1-是',
  `version_remark` varchar(255) DEFAULT NULL COMMENT '版本说明',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_version_no` (`template_id`,`version_no`),
  KEY `idx_template_version_current` (`template_id`,`current_published`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板版本表';

CREATE TABLE IF NOT EXISTS `template_render_record` (
  `id` bigint NOT NULL COMMENT '渲染记录ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(128) NOT NULL COMMENT '模板编码',
  `version_id` bigint NOT NULL COMMENT '模板版本ID',
  `version_no` int NOT NULL COMMENT '模板版本号',
  `output_format` varchar(32) NOT NULL COMMENT '输出格式',
  `status` varchar(32) NOT NULL COMMENT '状态: PENDING RUNNING SUCCESS FAILED',
  `output_file_id` bigint DEFAULT NULL COMMENT '输出文件ID',
  `output_content` longtext DEFAULT NULL COMMENT '文本输出内容',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '失败原因',
  `variable_payload` json DEFAULT NULL COMMENT '渲染变量快照',
  `biz_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `biz_id` varchar(128) DEFAULT NULL COMMENT '业务ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_render_template` (`tenant_id`,`template_code`),
  KEY `idx_template_render_status` (`tenant_id`,`status`),
  KEY `idx_template_render_biz` (`tenant_id`,`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板渲染记录表';

-- -----------------------------------------------------------------------------
-- Squashed from: V2__add_template_business_scope.sql
-- -----------------------------------------------------------------------------
ALTER TABLE `template`
  ADD COLUMN `business_group` varchar(64) DEFAULT NULL COMMENT '业务组编码' AFTER `category_name`,
  ADD COLUMN `business_type` varchar(64) DEFAULT NULL COMMENT '业务类型' AFTER `business_group`,
  ADD COLUMN `business_key` varchar(128) DEFAULT NULL COMMENT '业务Key' AFTER `business_type`;

CREATE INDEX `idx_template_business_scope`
  ON `template` (`tenant_id`, `business_group`, `business_type`, `business_key`);

-- -----------------------------------------------------------------------------
-- Squashed from: V3__template_version_source_format.sql
-- -----------------------------------------------------------------------------
ALTER TABLE `template`
  MODIFY COLUMN `source_format` varchar(32) DEFAULT NULL COMMENT '当前发布内容稿源格式: TEXT HTML DOCX XLSX';

UPDATE `template_version`
SET `source_format` = 'TEXT'
WHERE `source_format` IS NULL OR `source_format` = '';

-- -----------------------------------------------------------------------------
-- Squashed from: V4__template_business_key_unique.sql
-- -----------------------------------------------------------------------------
ALTER TABLE `template`
  ADD UNIQUE KEY `uk_template_tenant_business_key` (`tenant_id`, `business_key`);

-- -----------------------------------------------------------------------------
-- Squashed from: V5__template_draft_publish_state.sql
-- -----------------------------------------------------------------------------
ALTER TABLE `template`
  ADD COLUMN `draft_source_format` varchar(32) DEFAULT NULL COMMENT '未发布草稿源格式: TEXT HTML DOCX XLSX' AFTER `current_version_no`,
  ADD COLUMN `draft_content` longtext DEFAULT NULL COMMENT '未发布草稿文本或HTML内容' AFTER `draft_source_format`,
  ADD COLUMN `draft_source_file_id` bigint DEFAULT NULL COMMENT '未发布草稿文档模板源文件ID' AFTER `draft_content`,
  ADD COLUMN `draft_variable_schema` json DEFAULT NULL COMMENT '未发布草稿变量定义' AFTER `draft_source_file_id`,
  ADD COLUMN `has_unpublished_changes` tinyint NOT NULL DEFAULT '0' COMMENT '是否存在未发布变更: 0-否 1-是' AFTER `draft_variable_schema`;
