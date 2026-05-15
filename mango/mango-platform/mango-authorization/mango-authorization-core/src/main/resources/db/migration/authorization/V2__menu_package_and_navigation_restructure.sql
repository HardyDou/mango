CREATE TABLE IF NOT EXISTS `authorization_menu_package` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `package_name` varchar(100) NOT NULL COMMENT '套餐名称',
  `package_code` varchar(64) NOT NULL COMMENT '套餐编码',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0-禁用,1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记: 0-正常, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_menu_package_code` (`tenant_id`,`package_code`),
  KEY `idx_authorization_menu_package_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单授权套餐主档';

CREATE TABLE IF NOT EXISTS `authorization_menu_package_item` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `package_id` bigint NOT NULL COMMENT '套餐ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_menu_package_item` (`tenant_id`,`package_id`,`menu_id`),
  KEY `idx_authorization_menu_package_item_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单授权套餐-菜单关联表';

DELETE FROM `authorization_role_menu` WHERE `menu_id` IN (21, 21000, 21001, 21002, 21003, 21004);
DELETE FROM `authorization_menu` WHERE `id` IN (21, 21000, 21001, 21002, 21003, 21004);

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(25,1,'internal-admin',2,2,'套餐管理','system:menu-package','/system/menu-package','Tickets','@/views/system/menu-package/index.vue',1,1,1,0,0,NULL,'system:menu-package:list',NULL,NULL,NOW(),NOW(),'菜单授权套餐管理',0,NULL,NOW(),NULL,NOW()),
(1200,1,'internal-admin',12,3,'查询机构列表','system:tenant:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:tenant:list',NULL,NULL,NOW(),NOW(),'机构列表查询权限',0,NULL,NOW(),NULL,NOW()),
(1400,1,'internal-admin',14,3,'查询应用列表','authorization:app:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'authorization:app:list',NULL,NULL,NOW(),NOW(),'授权应用列表查询权限',0,NULL,NOW(),NULL,NOW()),
(4000,1,'internal-admin',4,3,'查询菜单列表','system:menu:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:menu:list',NULL,NULL,NOW(),NOW(),'菜单列表查询权限',0,NULL,NOW(),NULL,NOW()),
(6000,1,'internal-admin',6,3,'查询系统配置列表','system:config:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:config:list',NULL,NULL,NOW(),NOW(),'系统配置列表查询权限',0,NULL,NOW(),NULL,NOW()),
(7000,1,'internal-admin',7,3,'查询字典类型列表','system:dict:type:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:dict:type:list',NULL,NULL,NOW(),NOW(),'字典类型列表查询权限',0,NULL,NOW(),NULL,NOW()),
(7001,1,'internal-admin',7,3,'查询字典类型','system:dict:type:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:dict:type:query',NULL,NULL,NOW(),NOW(),'字典类型详情查询权限',0,NULL,NOW(),NULL,NOW()),
(7002,1,'internal-admin',7,3,'新增字典类型','system:dict:type:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:dict:type:add',NULL,NULL,NOW(),NOW(),'字典类型新增权限',0,NULL,NOW(),NULL,NOW()),
(7003,1,'internal-admin',7,3,'修改字典类型','system:dict:type:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:dict:type:edit',NULL,NULL,NOW(),NOW(),'字典类型修改权限',0,NULL,NOW(),NULL,NOW()),
(7004,1,'internal-admin',7,3,'删除字典类型','system:dict:type:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:dict:type:delete',NULL,NULL,NOW(),NOW(),'字典类型删除权限',0,NULL,NOW(),NULL,NOW()),
(7010,1,'internal-admin',7,3,'查询字典数据列表','system:dict:data:list',NULL,NULL,NULL,10,1,0,0,0,NULL,'system:dict:data:list',NULL,NULL,NOW(),NOW(),'字典数据列表查询权限',0,NULL,NOW(),NULL,NOW()),
(7011,1,'internal-admin',7,3,'查询字典数据','system:dict:data:query',NULL,NULL,NULL,11,1,0,0,0,NULL,'system:dict:data:query',NULL,NULL,NOW(),NOW(),'字典数据详情查询权限',0,NULL,NOW(),NULL,NOW()),
(7012,1,'internal-admin',7,3,'新增字典数据','system:dict:data:add',NULL,NULL,NULL,12,1,0,0,0,NULL,'system:dict:data:add',NULL,NULL,NOW(),NOW(),'字典数据新增权限',0,NULL,NOW(),NULL,NOW()),
(7013,1,'internal-admin',7,3,'修改字典数据','system:dict:data:edit',NULL,NULL,NULL,13,1,0,0,0,NULL,'system:dict:data:edit',NULL,NULL,NOW(),NOW(),'字典数据修改权限',0,NULL,NOW(),NULL,NOW()),
(7014,1,'internal-admin',7,3,'删除字典数据','system:dict:data:delete',NULL,NULL,NULL,14,1,0,0,0,NULL,'system:dict:data:delete',NULL,NULL,NOW(),NOW(),'字典数据删除权限',0,NULL,NOW(),NULL,NOW()),
(25000,1,'internal-admin',25,3,'查询套餐列表','system:menu-package:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:menu-package:list',NULL,NULL,NOW(),NOW(),'套餐列表查询权限',0,NULL,NOW(),NULL,NOW()),
(25001,1,'internal-admin',25,3,'查询套餐','system:menu-package:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:menu-package:query',NULL,NULL,NOW(),NOW(),'套餐详情查询权限',0,NULL,NOW(),NULL,NOW()),
(25002,1,'internal-admin',25,3,'新增套餐','system:menu-package:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:menu-package:add',NULL,NULL,NOW(),NOW(),'套餐新增权限',0,NULL,NOW(),NULL,NOW()),
(25003,1,'internal-admin',25,3,'修改套餐','system:menu-package:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:menu-package:edit',NULL,NULL,NOW(),NOW(),'套餐修改权限',0,NULL,NOW(),NULL,NOW()),
(25004,1,'internal-admin',25,3,'删除套餐','system:menu-package:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:menu-package:delete',NULL,NULL,NOW(),NOW(),'套餐删除权限',0,NULL,NOW(),NULL,NOW()),
(26,1,'internal-admin',0,1,'协同办公','workflow','/workflow','Promotion',NULL,2,1,1,0,0,'/workflow/task/todo',NULL,NULL,NULL,NOW(),NOW(),'协同办公工作台入口',0,NULL,NOW(),NULL,NOW()),
(2601,1,'internal-admin',26,1,'任务管理','workflow:task','/workflow/task','List',NULL,1,1,1,0,0,'/workflow/task/todo',NULL,NULL,NULL,NOW(),NOW(),'协同办公任务中心',0,NULL,NOW(),NULL,NOW()),
(260101,1,'internal-admin',2601,2,'我的待办','workflow:task:todo','/workflow/task/todo','Tickets','@/views/workflow/task/todo/index.vue',1,1,1,0,0,NULL,'workflow:task:list',NULL,NULL,NOW(),NOW(),'当前用户待办流程任务',0,NULL,NOW(),NULL,NOW()),
(260102,1,'internal-admin',2601,2,'我的发起','workflow:task:initiated','/workflow/task/initiated','Position','@/views/workflow/task/initiated/index.vue',2,1,1,0,0,NULL,'workflow:task:list',NULL,NULL,NOW(),NOW(),'当前用户发起的流程',0,NULL,NOW(),NULL,NOW()),
(260103,1,'internal-admin',2601,2,'我的已办','workflow:task:done','/workflow/task/done','CircleCheck','@/views/workflow/task/done/index.vue',3,1,1,0,0,NULL,'workflow:task:list',NULL,NULL,NOW(),NOW(),'当前用户已办流程任务',0,NULL,NOW(),NULL,NOW()),
(260104,1,'internal-admin',2601,2,'抄送给我','workflow:task:copied','/workflow/task/copied','Message','@/views/workflow/task/copied/index.vue',4,1,1,0,0,NULL,'workflow:task:list',NULL,NULL,NOW(),NOW(),'抄送给当前用户的流程事项',0,NULL,NOW(),NULL,NOW()),
(2601000,1,'internal-admin',2601,3,'查询协同任务','workflow:task:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'workflow:task:list',NULL,NULL,NOW(),NOW(),'协同办公任务列表查询权限',0,NULL,NOW(),NULL,NOW()),
(2602,1,'internal-admin',26,2,'发起流程','workflow:start-process','/workflow/start-process','Promotion','@/views/workflow/start-process/index.vue',2,1,1,0,0,NULL,'system:workflow:list',NULL,NULL,NOW(),NOW(),'选择已发布流程并发起',0,NULL,NOW(),NULL,NOW()),
(2603,1,'internal-admin',26,2,'业务表单','workflow:business-form','/workflow/business-form','Document','@/views/workflow/business-form/index.vue',4,1,1,0,0,NULL,'system:workflow:list',NULL,NULL,NOW(),NOW(),'流程发起表单清单',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
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
`del_flag` = VALUES(`del_flag`);

UPDATE `authorization_menu` SET `redirect`='/system/menu-package' WHERE `id`=1;
UPDATE `authorization_menu` SET `menu_name`='权限管理', `menu_code`='system:permission', `path`='/system/permission', `icon`='Lock', `redirect`='/system/menu-package', `remark`='机构、成员、角色、菜单与套餐权限管理', `status`=1, `visible`=1 WHERE `id`=2;
UPDATE `authorization_menu` SET `parent_id`=2, `sort`=1, `status`=1, `visible`=1 WHERE `id`=25;
UPDATE `authorization_menu` SET `parent_id`=2, `sort`=2, `status`=1, `visible`=1 WHERE `id`=12;
UPDATE `authorization_menu` SET `parent_id`=2, `sort`=3, `status`=1, `visible`=1 WHERE `id`=17;
UPDATE `authorization_menu` SET `parent_id`=2, `sort`=4, `status`=1, `visible`=1 WHERE `id`=15;
UPDATE `authorization_menu` SET `parent_id`=2, `sort`=5, `status`=1, `visible`=1 WHERE `id`=20;
UPDATE `authorization_menu` SET `parent_id`=2, `sort`=6, `status`=1, `visible`=1 WHERE `id`=3;
UPDATE `authorization_menu` SET `parent_id`=2, `sort`=7, `status`=1, `visible`=1 WHERE `id`=4;
UPDATE `authorization_menu` SET `parent_id`=1, `sort`=2, `status`=1, `visible`=1 WHERE `id`=14;
UPDATE `authorization_menu` SET `parent_id`=1, `menu_type`=2, `menu_name`='字典管理', `menu_code`='system:dict', `path`='/system/dict', `icon`='Collection', `component`='@/views/system/dict/index.vue', `sort`=3, `redirect`=NULL, `permissions`='system:dict:list', `remark`='字典类型与字典数据管理', `status`=1, `visible`=1 WHERE `id`=7;
UPDATE `authorization_menu` SET `parent_id`=1, `sort`=4, `status`=1, `visible`=1 WHERE `id`=6;
UPDATE `authorization_menu` SET `parent_id`=1, `sort`=5, `status`=1, `visible`=1 WHERE `id`=16;
UPDATE `authorization_menu` SET `menu_name`='日志管理', `menu_code`='system:log', `path`='/system/log', `icon`='DocumentChecked', `sort`=6, `redirect`='/system/login-log', `remark`='登录与操作审计日志', `status`=1, `visible`=1 WHERE `id`=8;
UPDATE `authorization_menu` SET `status`=0, `visible`=0, `redirect`=NULL WHERE `id` IN (5,18,19,22,23,27);

INSERT INTO `authorization_menu_package` (`id`, `tenant_id`, `package_name`, `package_code`, `app_code`, `status`, `sort`, `remark`, `create_by`, `update_by`, `create_time`, `update_time`, `del_flag`)
VALUES
(1,1,'平台管理套餐','platform_admin','internal-admin',1,1,'平台管理员默认菜单授权套餐',NULL,NULL,NOW(),NOW(),0),
(2,1,'机构协同套餐','institution_collaboration','internal-admin',1,2,'普通机构后台默认菜单授权套餐',NULL,NULL,NOW(),NOW(),0)
ON DUPLICATE KEY UPDATE
`package_name` = VALUES(`package_name`),
`app_code` = VALUES(`app_code`),
`status` = VALUES(`status`),
`sort` = VALUES(`sort`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`);

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1001,1,1,1,1),(1002,1,1,2,2),(1003,1,1,25,3),(1004,1,1,12,4),(1005,1,1,17,5),(1006,1,1,15,6),(1007,1,1,20,7),(1008,1,1,3,8),(1009,1,1,4,9),(1010,1,1,14,10),(1011,1,1,7,11),(1012,1,1,6,12),(1013,1,1,16,13),(1014,1,1,8,14),(1015,1,1,9,15),(1016,1,1,10,16),(1017,1,1,26,17),(1018,1,1,2601,18),(1019,1,1,260101,19),(1020,1,1,260102,20),(1021,1,1,260103,21),(1022,1,1,260104,22),(1023,1,1,2602,23),(1024,1,1,24,24),(1025,1,1,2603,25),
(1026,1,1,1200,26),(1027,1,1,1201,27),(1028,1,1,1202,28),(1029,1,1,1203,29),(1030,1,1,1204,30),
(1031,1,1,1500,31),(1032,1,1,1501,32),(1033,1,1,1502,33),(1034,1,1,1503,34),(1035,1,1,1504,35),
(1036,1,1,1700,36),(1037,1,1,1701,37),(1038,1,1,1702,38),(1039,1,1,1703,39),(1040,1,1,1704,40),
(1041,1,1,2000,41),(1042,1,1,2001,42),(1043,1,1,2002,43),(1044,1,1,2003,44),(1045,1,1,2004,45),(1046,1,1,2005,46),(1047,1,1,2006,47),(1048,1,1,2007,48),
(1049,1,1,3000,49),(1050,1,1,3001,50),(1051,1,1,3002,51),(1052,1,1,3003,52),(1053,1,1,3004,53),(1054,1,1,3005,54),
(1055,1,1,4000,55),(1056,1,1,4001,56),(1057,1,1,4002,57),(1058,1,1,4003,58),(1059,1,1,4004,59),
(1060,1,1,1400,60),(1061,1,1,1401,61),(1062,1,1,1402,62),(1063,1,1,1403,63),(1064,1,1,1404,64),
(1065,1,1,6000,65),(1066,1,1,6001,66),(1067,1,1,6002,67),(1068,1,1,6003,68),(1069,1,1,6004,69),
(1070,1,1,7000,70),(1071,1,1,7001,71),(1072,1,1,7002,72),(1073,1,1,7003,73),(1074,1,1,7004,74),(1075,1,1,7010,75),(1076,1,1,7011,76),(1077,1,1,7012,77),(1078,1,1,7013,78),(1079,1,1,7014,79),
(1080,1,1,9002,80),(1081,1,1,9003,81),(1082,1,1,9004,82),(1083,1,1,10002,83),(1084,1,1,10003,84),(1085,1,1,10004,85),(1086,1,1,24000,86),(1087,1,1,2601000,87),(1088,1,1,2601001,88),(1089,1,1,2601002,89),(1090,1,1,2601003,90),(1091,1,1,2602001,91),(1092,1,1,25000,92),(1093,1,1,25001,93),(1094,1,1,25002,94),(1095,1,1,25003,95),(1096,1,1,25004,96),
(2001,1,2,1,1),(2002,1,2,2,2),(2003,1,2,17,3),(2004,1,2,15,4),(2005,1,2,20,5),(2006,1,2,3,6),(2007,1,2,8,7),(2008,1,2,9,8),(2009,1,2,10,9),(2010,1,2,26,10),(2011,1,2,2601,11),(2012,1,2,260101,12),(2013,1,2,260102,13),(2014,1,2,260103,14),(2015,1,2,260104,15),(2016,1,2,2602,16),(2017,1,2,24,17),(2018,1,2,2603,18),(2019,1,2,2601000,19),(2020,1,2,24000,20);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 500000 + `menu_id`, 1, 1, `menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu_package_item`
WHERE `package_id` = 1;

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 600000 + `menu_id`, 2, 2, `menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu_package_item`
WHERE `package_id` = 2;

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 700000 + `menu_id`, 3, 3, `menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu_package_item`
WHERE `package_id` = 2;

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 800000 + `menu_id`, 4, 4, `menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu_package_item`
WHERE `package_id` = 2;
