DELETE FROM `sys_dict_data`
WHERE `dict_type` IN (
  'authorization_role_type',
  'authorization_menu_type',
  'auth_realm',
  'auth_actor_type',
  'template_source_format',
  'template_output_format',
  'template_render_status',
  'file_access_level'
);

DELETE FROM `sys_dict_type`
WHERE `dict_type` IN (
  'authorization_role_type',
  'authorization_menu_type',
  'auth_realm',
  'auth_actor_type',
  'template_source_format',
  'template_output_format',
  'template_render_status',
  'file_access_level'
);
