ALTER TABLE `file_settings`
  ADD COLUMN `preview_max_size` bigint NOT NULL DEFAULT '209715200' COMMENT 'Office 文档在线预览最大大小' AFTER `preview_expire_seconds`;
