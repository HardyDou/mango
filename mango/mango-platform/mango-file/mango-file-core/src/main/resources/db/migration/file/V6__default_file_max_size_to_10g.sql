ALTER TABLE `file_settings`
  MODIFY COLUMN `max_size` bigint NOT NULL DEFAULT '10737418240' COMMENT '单文件最大大小';

UPDATE `file_settings`
SET `max_size` = 10737418240
WHERE `max_size` = 104857600;
