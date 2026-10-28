ALTER TABLE `sys_dict_data`
  ADD COLUMN `dict_groups` varchar(255) DEFAULT NULL COMMENT '分组标记，逗号分隔，空值表示未分组' AFTER `parent_value`;
