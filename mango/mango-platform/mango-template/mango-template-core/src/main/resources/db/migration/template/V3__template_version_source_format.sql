ALTER TABLE `template`
  MODIFY COLUMN `source_format` varchar(32) DEFAULT NULL COMMENT '当前发布内容稿源格式: TEXT HTML DOCX XLSX';

UPDATE `template_version`
SET `source_format` = 'TEXT'
WHERE `source_format` IS NULL OR `source_format` = '';
