ALTER TABLE `file_settings`
    ADD COLUMN `access_mode` varchar(32) NOT NULL DEFAULT 'PROXY' COMMENT '文件访问模式: PROXY-Java服务转发 DIRECT-直连底层存储' AFTER `public_read_requires_token`;

UPDATE `file_settings`
SET `access_mode` = COALESCE(NULLIF(`access_mode`, ''), 'PROXY');
