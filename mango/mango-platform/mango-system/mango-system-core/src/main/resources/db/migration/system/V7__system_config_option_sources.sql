ALTER TABLE `sys_config`
  ADD COLUMN `option_source` varchar(32) NOT NULL DEFAULT 'CUSTOM' COMMENT '选项来源：CUSTOM/DICT' AFTER `options`,
  ADD COLUMN `dict_type` varchar(50) DEFAULT NULL COMMENT '绑定字典类型' AFTER `option_source`;
