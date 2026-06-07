INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (9, 'internal-admin', 'mango-job', '任务调度模块', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `module_code` = VALUES(`module_code`),
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (9, 'internal-admin', 'mango-job', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19, 'internal-admin', 'mango-job', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (29, 'internal-admin', 'mango-job', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2950,1,'internal-admin','mango-job',2700,1,'任务管理','job','/job','Timer',NULL,9,1,1,0,0,'/job/definition',NULL,NULL,NULL,NOW(),NOW(),'Mango 原生任务调度管理入口',0,NULL,NOW(),NULL,NOW()),
  (2951,1,'internal-admin','mango-job',2950,2,'任务定义','job:definition','/job/definition','List','@/views/job/definition/index.vue',1,1,1,0,0,NULL,'job:definition:list',NULL,NULL,NOW(),NOW(),'任务定义、调度表达式、处理器和引擎同步状态管理',0,NULL,NOW(),NULL,NOW()),
  (2952,1,'internal-admin','mango-job',2950,2,'执行实例','job:instance','/job/instance','Tickets','@/views/job/instance/index.vue',2,1,1,0,0,NULL,'job:instance:list',NULL,NULL,NOW(),NOW(),'任务执行实例查询',0,NULL,NOW(),NULL,NOW()),
  (2953,1,'internal-admin','mango-job',2950,2,'执行日志','job:log','/job/log','Document','@/views/job/log/index.vue',3,1,1,0,0,NULL,'job:log:list',NULL,NULL,NOW(),NOW(),'任务日志索引查询',0,NULL,NOW(),NULL,NOW()),
  (2954,1,'internal-admin','mango-job',2950,2,'Worker','job:worker','/job/worker','Monitor','@/views/job/worker/index.vue',4,1,1,0,0,NULL,'job:worker:list',NULL,NULL,NOW(),NOW(),'任务执行 Worker 心跳快照查询',0,NULL,NOW(),NULL,NOW()),
  (2955,1,'internal-admin','mango-job',2950,2,'处理器','job:handler','/job/handler','Connection','@/views/job/handler/index.vue',5,1,1,0,0,NULL,'job:handler:list',NULL,NULL,NOW(),NOW(),'当前应用注册的任务处理器清单',0,NULL,NOW(),NULL,NOW()),
  (2956,1,'internal-admin','mango-job',2950,2,'引擎状态','job:engine','/job/engine','Cpu','@/views/job/engine/index.vue',6,1,1,0,0,NULL,'job:engine:list',NULL,NULL,NOW(),NOW(),'底层调度引擎同步状态汇总',0,NULL,NOW(),NULL,NOW()),
  (295101,1,'internal-admin','mango-job',2951,3,'任务查询','job:definition:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'job:definition:list',NULL,NULL,NOW(),NOW(),'任务定义列表查询权限',0,NULL,NOW(),NULL,NOW()),
  (295102,1,'internal-admin','mango-job',2951,3,'任务详情','job:definition:query',NULL,NULL,NULL,2,1,0,0,0,NULL,'job:definition:query',NULL,NULL,NOW(),NOW(),'任务定义详情查询权限',0,NULL,NOW(),NULL,NOW()),
  (295103,1,'internal-admin','mango-job',2951,3,'新增任务','job:definition:add',NULL,NULL,NULL,3,1,0,0,0,NULL,'job:definition:add',NULL,NULL,NOW(),NOW(),'任务定义新增权限',0,NULL,NOW(),NULL,NOW()),
  (295104,1,'internal-admin','mango-job',2951,3,'编辑任务','job:definition:edit',NULL,NULL,NULL,4,1,0,0,0,NULL,'job:definition:edit',NULL,NULL,NOW(),NOW(),'任务定义编辑权限',0,NULL,NOW(),NULL,NOW()),
  (295105,1,'internal-admin','mango-job',2951,3,'删除任务','job:definition:delete',NULL,NULL,NULL,5,1,0,0,0,NULL,'job:definition:delete',NULL,NULL,NOW(),NOW(),'任务定义删除权限',0,NULL,NOW(),NULL,NOW()),
  (295106,1,'internal-admin','mango-job',2951,3,'调整状态','job:definition:status',NULL,NULL,NULL,6,1,0,0,0,NULL,'job:definition:status',NULL,NULL,NOW(),NOW(),'任务定义启用、暂停、禁用权限',0,NULL,NOW(),NULL,NOW()),
  (295107,1,'internal-admin','mango-job',2951,3,'手动触发','job:definition:trigger',NULL,NULL,NULL,7,1,0,0,0,NULL,'job:definition:trigger',NULL,NULL,NOW(),NOW(),'手动触发任务权限',0,NULL,NOW(),NULL,NOW()),
  (295201,1,'internal-admin','mango-job',2952,3,'实例查询','job:instance:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'job:instance:list',NULL,NULL,NOW(),NOW(),'执行实例列表查询权限',0,NULL,NOW(),NULL,NOW()),
  (295301,1,'internal-admin','mango-job',2953,3,'日志查询','job:log:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'job:log:list',NULL,NULL,NOW(),NOW(),'执行日志索引查询权限',0,NULL,NOW(),NULL,NOW()),
  (295401,1,'internal-admin','mango-job',2954,3,'Worker 查询','job:worker:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'job:worker:list',NULL,NULL,NOW(),NOW(),'Worker 快照查询权限',0,NULL,NOW(),NULL,NOW()),
  (295501,1,'internal-admin','mango-job',2955,3,'处理器查询','job:handler:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'job:handler:list',NULL,NULL,NOW(),NOW(),'处理器清单查询权限',0,NULL,NOW(),NULL,NOW()),
  (295601,1,'internal-admin','mango-job',2956,3,'引擎状态查询','job:engine:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'job:engine:list',NULL,NULL,NOW(),NOW(),'引擎同步状态查询权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
  `module_code` = VALUES(`module_code`),
  `parent_id` = VALUES(`parent_id`),
  `menu_type` = VALUES(`menu_type`),
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `redirect` = VALUES(`redirect`),
  `permissions` = VALUES(`permissions`),
  `remark` = VALUES(`remark`),
  `del_flag` = VALUES(`del_flag`),
  `update_time` = NOW(),
  `updated_at` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
  (12950,1,1,2950,90),(12951,1,1,2951,91),(12952,1,1,2952,92),(12953,1,1,2953,93),(12954,1,1,2954,94),(12955,1,1,2955,95),(12956,1,1,2956,96),
  (1295101,1,1,295101,101),(1295102,1,1,295102,102),(1295103,1,1,295103,103),(1295104,1,1,295104,104),(1295105,1,1,295105,105),(1295106,1,1,295106,106),(1295107,1,1,295107,107),
  (1295201,1,1,295201,108),(1295301,1,1,295301,109),(1295401,1,1,295401,110),(1295501,1,1,295501,111),(1295601,1,1,295601,112),
  (22950,1,2,2950,90),(22951,1,2,2951,91),(22952,1,2,2952,92),(22953,1,2,2953,93),(22954,1,2,2954,94),(22955,1,2,2955,95),(22956,1,2,2956,96),
  (2295101,1,2,295101,101),(2295102,1,2,295102,102),(2295103,1,2,295103,103),(2295104,1,2,295104,104),(2295105,1,2,295105,105),(2295106,1,2,295106,106),(2295107,1,2,295107,107),
  (2295201,1,2,295201,108),(2295301,1,2,295301,109),(2295401,1,2,295401,110),(2295501,1,2,295501,111),(2295601,1,2,295601,112);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
  (52950,1,1,2950,NOW(),NULL,NOW(),NULL,NOW()),(52951,1,1,2951,NOW(),NULL,NOW(),NULL,NOW()),(52952,1,1,2952,NOW(),NULL,NOW(),NULL,NOW()),(52953,1,1,2953,NOW(),NULL,NOW(),NULL,NOW()),(52954,1,1,2954,NOW(),NULL,NOW(),NULL,NOW()),(52955,1,1,2955,NOW(),NULL,NOW(),NULL,NOW()),(52956,1,1,2956,NOW(),NULL,NOW(),NULL,NOW()),
  (5295101,1,1,295101,NOW(),NULL,NOW(),NULL,NOW()),(5295102,1,1,295102,NOW(),NULL,NOW(),NULL,NOW()),(5295103,1,1,295103,NOW(),NULL,NOW(),NULL,NOW()),(5295104,1,1,295104,NOW(),NULL,NOW(),NULL,NOW()),(5295105,1,1,295105,NOW(),NULL,NOW(),NULL,NOW()),(5295106,1,1,295106,NOW(),NULL,NOW(),NULL,NOW()),(5295107,1,1,295107,NOW(),NULL,NOW(),NULL,NOW()),
  (5295201,1,1,295201,NOW(),NULL,NOW(),NULL,NOW()),(5295301,1,1,295301,NOW(),NULL,NOW(),NULL,NOW()),(5295401,1,1,295401,NOW(),NULL,NOW(),NULL,NOW()),(5295501,1,1,295501,NOW(),NULL,NOW(),NULL,NOW()),(5295601,1,1,295601,NOW(),NULL,NOW(),NULL,NOW());
