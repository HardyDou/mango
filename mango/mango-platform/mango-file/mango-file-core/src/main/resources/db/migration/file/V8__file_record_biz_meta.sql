ALTER TABLE `file_record`
    ADD COLUMN `biz_meta` json DEFAULT NULL COMMENT '业务自定义参数JSON' AFTER `purpose`;
