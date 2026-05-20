UPDATE `authorization_menu`
SET `menu_name` = '审批中心',
    `icon` = 'Stamp',
    `redirect` = '/workflow/start-process',
    `remark` = '流程发起、审批办理和流程配置入口',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `menu_code` = 'workflow';

UPDATE `authorization_menu`
SET `menu_name` = '流程办理',
    `icon` = 'Tickets',
    `redirect` = '/workflow/start-process',
    `remark` = '流程发起、待办、已办和抄送事项',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `menu_code` = 'workflow:task';

UPDATE `authorization_menu`
SET `parent_id` = 2601,
    `sort` = 1,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `menu_code` = 'workflow:start-process';

UPDATE `authorization_menu`
SET `sort` = CASE `menu_code`
        WHEN 'workflow:task:todo' THEN 2
        WHEN 'workflow:task:initiated' THEN 3
        WHEN 'workflow:task:done' THEN 4
        WHEN 'workflow:task:copied' THEN 5
        ELSE `sort`
    END,
    `menu_name` = CASE `menu_code`
        WHEN 'workflow:task:initiated' THEN '我的申请'
        ELSE `menu_name`
    END,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `menu_code` IN ('workflow:task:todo', 'workflow:task:initiated', 'workflow:task:done', 'workflow:task:copied');

UPDATE `authorization_app_module`
SET `module_name` = '审批中心模块',
    `update_time` = NOW()
WHERE `app_code` = 'internal-admin'
  AND `module_code` = 'mango-workflow';
