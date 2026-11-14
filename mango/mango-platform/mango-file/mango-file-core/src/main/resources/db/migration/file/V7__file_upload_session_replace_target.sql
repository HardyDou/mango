ALTER TABLE `file_upload_session`
  ADD COLUMN `replace_file_id` bigint DEFAULT NULL COMMENT '替换目标文件ID，非空表示完成会话时替换该文件内容' AFTER `file_record_id`;
