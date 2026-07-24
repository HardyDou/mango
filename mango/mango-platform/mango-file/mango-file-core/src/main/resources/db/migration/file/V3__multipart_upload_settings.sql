ALTER TABLE `file_settings`
  ADD COLUMN `multipart_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用大文件分片上传' AFTER `instant_upload_enabled`,
  ADD COLUMN `multipart_threshold` bigint NOT NULL DEFAULT '20971520' COMMENT '大文件分片上传临界值' AFTER `multipart_enabled`;

ALTER TABLE `file_upload_session`
  MODIFY COLUMN `file_hash` varchar(128) NULL COMMENT '文件SHA-256哈希，客户端无法计算时由服务端补算';
