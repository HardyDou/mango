UPDATE `workflow_definition_version` v
JOIN `workflow_definition` d ON d.`id` = v.`definition_id`
SET
  v.`category_id` = COALESCE(v.`category_id`, d.`category_id`),
  v.`org_id` = COALESCE(v.`org_id`, d.`org_id`),
  v.`admin_users` = COALESCE(v.`admin_users`, d.`admin_users`),
  v.`icon` = COALESCE(v.`icon`, d.`icon`),
  v.`definition_name` = COALESCE(v.`definition_name`, d.`definition_name`),
  v.`definition_key` = COALESCE(v.`definition_key`, d.`definition_key`),
  v.`remark` = COALESCE(v.`remark`, d.`remark`),
  v.`form_code` = COALESCE(v.`form_code`, d.`form_code`);
