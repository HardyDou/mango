UPDATE `authorization_menu`
SET `menu_name` = 'Worker 节点',
    `remark` = '任务执行 Worker 节点查询和治理',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-job'
  AND `menu_code` = 'job:worker';

UPDATE `authorization_menu`
SET `menu_name` = '运行状态',
    `remark` = 'Mango 原生任务调度运行状态汇总',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `app_code` = 'internal-admin'
  AND `module_code` = 'mango-job'
  AND `menu_code` = 'job:engine';
