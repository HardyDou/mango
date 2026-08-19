ALTER TABLE `identity_external_binding`
  ADD COLUMN `avatar_file_id` bigint DEFAULT NULL COMMENT '第三方头像文件ID' AFTER `display_name`;
