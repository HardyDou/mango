ALTER TABLE `file_settings`
  MODIFY COLUMN `access_mode` varchar(32) NOT NULL DEFAULT 'PROXY' COMMENT '文件访问模式';
