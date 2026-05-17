UPDATE `authorization_menu`
SET `icon` = CASE `menu_code`
    WHEN 'workflow' THEN 'Promotion'
    WHEN 'workflow:task' THEN 'List'
    WHEN 'workflow:task:todo' THEN 'Tickets'
    WHEN 'workflow:task:initiated' THEN 'Position'
    WHEN 'workflow:task:done' THEN 'CircleCheck'
    WHEN 'workflow:task:copied' THEN 'Message'
    WHEN 'workflow:start-process' THEN 'Promotion'
    WHEN 'system:workflow' THEN 'Operation'
    WHEN 'workflow:business-form' THEN 'Document'
    ELSE `icon`
  END,
  `update_time` = NOW(),
  `updated_at` = NOW()
WHERE `menu_code` IN (
  'workflow',
  'workflow:task',
  'workflow:task:todo',
  'workflow:task:initiated',
  'workflow:task:done',
  'workflow:task:copied',
  'workflow:start-process',
  'system:workflow',
  'workflow:business-form'
);
