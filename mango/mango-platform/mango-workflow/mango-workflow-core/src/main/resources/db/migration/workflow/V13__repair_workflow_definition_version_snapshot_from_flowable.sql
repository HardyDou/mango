UPDATE `workflow_definition_version` v
JOIN `ACT_RE_PROCDEF` p ON p.`ID_` = v.`process_definition_id`
SET
  v.`definition_name` = p.`NAME_`,
  v.`definition_key` = p.`KEY_`
WHERE v.`publish_status` = 'SUCCESS';
