ALTER TABLE `sys_dict_type`
  ADD COLUMN `shape` varchar(16) NOT NULL DEFAULT 'FLAT' COMMENT '字典形态：FLAT/TREE' AFTER `dict_name`;

ALTER TABLE `sys_dict_data`
  ADD COLUMN `parent_value` varchar(100) DEFAULT NULL COMMENT '父节点值，空值表示根节点' AFTER `dict_value`;

ALTER TABLE `sys_dict_data`
  ADD KEY `idx_sys_dict_data_parent` (`tenant_id`, `dict_type`, `parent_value`, `sort`);
