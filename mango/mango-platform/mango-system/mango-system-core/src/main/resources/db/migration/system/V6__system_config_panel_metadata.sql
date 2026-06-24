ALTER TABLE `sys_config`
  ADD COLUMN `value_type` varchar(32) DEFAULT 'STRING' COMMENT '配置值展示与编辑类型：BOOLEAN/STRING/NUMBER/RADIO/SELECT/MULTI_SELECT/DATE/DATE_RANGE' AFTER `domain_code`,
  ADD COLUMN `group_code` varchar(64) DEFAULT NULL COMMENT '配置分组编码' AFTER `value_type`,
  ADD COLUMN `group_name` varchar(100) DEFAULT NULL COMMENT '配置分组名称' AFTER `group_code`,
  ADD COLUMN `default_value` text DEFAULT NULL COMMENT '默认值' AFTER `group_name`,
  ADD COLUMN `options` text DEFAULT NULL COMMENT '选项列表，JSON字符串' AFTER `default_value`,
  ADD COLUMN `editable` tinyint NOT NULL DEFAULT '1' COMMENT '是否可编辑：0-否 1-是' AFTER `options`,
  ADD COLUMN `editable_reason` varchar(200) DEFAULT NULL COMMENT '不可编辑原因' AFTER `editable`;
