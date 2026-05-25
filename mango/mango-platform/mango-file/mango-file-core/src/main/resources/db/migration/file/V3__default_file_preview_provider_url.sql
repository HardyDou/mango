ALTER TABLE `file_settings`
    MODIFY COLUMN `preview_provider_url` varchar(500) DEFAULT '/file-preview/files/preview' COMMENT '外部文档预览服务地址';

UPDATE `file_settings`
SET `preview_provider_url` = '/file-preview/files/preview'
WHERE `preview_provider_url` IS NULL
   OR TRIM(`preview_provider_url`) = '';
