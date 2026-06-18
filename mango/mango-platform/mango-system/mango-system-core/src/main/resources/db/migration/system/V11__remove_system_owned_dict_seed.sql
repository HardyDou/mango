DELETE FROM `sys_dict_data`
WHERE `dict_type` IN (
  'sys_user_sex',
  'sys_normal_disable',
  'sys_login_type',
  'system_param_type',
  'system_config_type',
  'sys_login_status',
  'sys_operation_status',
  'org_type',
  'area_type',
  'area_status',
  'institution_type',
  'institution_capability',
  'institution_status'
);

DELETE FROM `sys_dict_type`
WHERE `dict_type` IN (
  'sys_user_sex',
  'sys_normal_disable',
  'sys_login_type',
  'system_param_type',
  'system_config_type',
  'sys_login_status',
  'sys_operation_status',
  'org_type',
  'area_type',
  'area_status',
  'institution_type',
  'institution_capability',
  'institution_status'
);
