ALTER TABLE `file_settings`
    ADD COLUMN `default_access_level` varchar(32) NOT NULL DEFAULT 'PRIVATE' COMMENT '默认访问级别' AFTER `blocked_extensions`,
    ADD COLUMN `duplicate_name_strategy` varchar(32) NOT NULL DEFAULT 'REJECT' COMMENT '重名处理策略: REJECT AUTO_RENAME ALLOW' AFTER `default_access_level`,
    ADD COLUMN `duplicate_check_directory_scoped` tinyint NOT NULL DEFAULT '1' COMMENT '是否按逻辑目录隔离重名' AFTER `duplicate_name_strategy`,
    ADD COLUMN `object_name_strategy` varchar(32) NOT NULL DEFAULT 'DATE_UUID' COMMENT '对象命名策略: DATE_UUID HASH ORIGINAL' AFTER `duplicate_check_directory_scoped`,
    ADD COLUMN `instant_upload_scope` varchar(32) NOT NULL DEFAULT 'TENANT' COMMENT '秒传匹配范围: TENANT GLOBAL' AFTER `instant_upload_enabled`,
    ADD COLUMN `content_type_check_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否校验内容类型' AFTER `instant_upload_scope`,
    ADD COLUMN `allowed_content_types` varchar(1000) DEFAULT NULL COMMENT '允许上传内容类型，逗号分隔；为空表示不限制' AFTER `content_type_check_enabled`,
    ADD COLUMN `blocked_content_types` varchar(1000) DEFAULT 'application/x-msdownload,application/x-sh' COMMENT '禁止上传内容类型，逗号分隔' AFTER `allowed_content_types`,
    ADD COLUMN `public_read_requires_token` tinyint NOT NULL DEFAULT '0' COMMENT '公开读取文件是否仍强制签名访问' AFTER `access_token_enabled`,
    ADD COLUMN `archive_retain_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否保留归档记录' AFTER `preview_external_extensions`,
    ADD COLUMN `archive_retain_days` int NOT NULL DEFAULT '180' COMMENT '归档记录保留天数' AFTER `archive_retain_enabled`,
    ADD COLUMN `archive_restore_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许恢复归档' AFTER `archive_retain_days`,
    ADD COLUMN `physical_delete_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除物理对象' AFTER `archive_restore_enabled`;

UPDATE `file_settings`
SET `default_access_level` = COALESCE(NULLIF(`default_access_level`, ''), 'PRIVATE'),
    `duplicate_name_strategy` = COALESCE(NULLIF(`duplicate_name_strategy`, ''), 'REJECT'),
    `duplicate_check_directory_scoped` = COALESCE(`duplicate_check_directory_scoped`, 1),
    `object_name_strategy` = COALESCE(NULLIF(`object_name_strategy`, ''), 'DATE_UUID'),
    `instant_upload_scope` = COALESCE(NULLIF(`instant_upload_scope`, ''), 'TENANT'),
    `content_type_check_enabled` = COALESCE(`content_type_check_enabled`, 1),
    `blocked_content_types` = COALESCE(NULLIF(`blocked_content_types`, ''), 'application/x-msdownload,application/x-sh'),
    `public_read_requires_token` = COALESCE(`public_read_requires_token`, 0),
    `archive_retain_enabled` = COALESCE(`archive_retain_enabled`, 1),
    `archive_retain_days` = COALESCE(`archive_retain_days`, 180),
    `archive_restore_enabled` = COALESCE(`archive_restore_enabled`, 0),
    `physical_delete_enabled` = COALESCE(`physical_delete_enabled`, 0);
