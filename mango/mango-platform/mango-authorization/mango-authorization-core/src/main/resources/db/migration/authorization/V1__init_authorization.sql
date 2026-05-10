
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_api_resource` (
  `id` bigint NOT NULL COMMENT '主键',
  `module_name` varchar(128) NOT NULL COMMENT '稳定模块名',
  `http_method` varchar(16) NOT NULL COMMENT 'HTTP 方法',
  `path_pattern` varchar(512) NOT NULL COMMENT '接口路径模式',
  `resource_code` varchar(640) NOT NULL COMMENT '资源编码',
  `permission_code` varchar(255) DEFAULT NULL COMMENT '权限编码',
  `access_mode` varchar(32) NOT NULL DEFAULT 'LOGIN' COMMENT '访问模式: PUBLIC/LOGIN/PERMISSION/INTERNAL',
  `handler_class` varchar(512) DEFAULT NULL COMMENT '处理类',
  `handler_method` varchar(128) DEFAULT NULL COMMENT '处理方法',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记: 0-正常, 1-已删除',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_api_resource_method_path` (`module_name`,`http_method`,`path_pattern`),
  KEY `idx_authorization_api_resource_module` (`module_name`),
  KEY `idx_authorization_api_resource_code` (`resource_code`),
  KEY `idx_authorization_api_resource_permission` (`permission_code`),
  KEY `idx_authorization_api_resource_access` (`access_mode`),
  KEY `idx_authorization_api_resource_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='接口资源表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_api_resource` WRITE;
/*!40000 ALTER TABLE `authorization_api_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `authorization_api_resource` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_permission` (
  `id` bigint NOT NULL COMMENT '主键',
  `perm_code` varchar(100) NOT NULL COMMENT '权限码，格式 model:module:action',
  `perm_name` varchar(50) NOT NULL COMMENT '权限名称',
  `perm_type` varchar(20) NOT NULL DEFAULT 'MENU' COMMENT '类型：MENU/BUTTON/API',
  `module` varchar(50) NOT NULL COMMENT '所属模块',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `perm_code` (`perm_code`),
  KEY `idx_authorization_permission_module` (`module`),
  KEY `idx_authorization_permission_type` (`perm_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_permission` WRITE;
/*!40000 ALTER TABLE `authorization_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `authorization_permission` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_role` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `app_code` varchar(64) NOT NULL DEFAULT 'internal-admin' COMMENT '应用编码',
  `realm` varchar(32) NOT NULL DEFAULT 'INTERNAL' COMMENT '登录域',
  `actor_type` varchar(32) DEFAULT NULL COMMENT '操作者类型',
  `role_code` varchar(100) NOT NULL COMMENT '角色标识',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_type` tinyint NOT NULL DEFAULT '1' COMMENT '角色类型: 1-系统角色 2-业务角色',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_role_tenant_app_role_code` (`tenant_id`,`app_code`,`role_code`),
  KEY `idx_authorization_role_tenant_id` (`tenant_id`),
  KEY `idx_authorization_role_app_code` (`app_code`),
  KEY `idx_authorization_role_realm_actor` (`realm`,`actor_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_role` WRITE;
/*!40000 ALTER TABLE `authorization_role` DISABLE KEYS */;
INSERT INTO `authorization_role` (`id`, `tenant_id`, `app_code`, `realm`, `actor_type`, `role_code`, `role_name`, `role_type`, `status`, `sort`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,1,'internal-admin','INTERNAL','INTERNAL_USER','ROLE_ADMIN','超级管理员',1,1,1,'2026-05-10 00:04:23','2026-05-10 00:04:23','芒果集团管理员',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(2,2,'internal-admin','INTERNAL','INTERNAL_USER','ROLE_ADMIN','超级管理员',1,1,1,'2026-05-10 00:04:23','2026-05-10 00:04:23','A公司管理员',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(3,3,'internal-admin','INTERNAL','INTERNAL_USER','ROLE_ADMIN','超级管理员',1,1,1,'2026-05-10 00:04:23','2026-05-10 00:04:23','B公司管理员',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(4,4,'internal-admin','INTERNAL','INTERNAL_USER','ROLE_ADMIN','超级管理员',1,1,1,'2026-05-10 00:04:23','2026-05-10 00:04:23','C公司管理员',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23');
/*!40000 ALTER TABLE `authorization_role` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_subject_role` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `subject_id` bigint NOT NULL COMMENT '主体ID',
  `subject_type` varchar(32) NOT NULL DEFAULT 'TENANT_MEMBER' COMMENT '主体类型',
  `app_code` varchar(64) DEFAULT NULL COMMENT '应用编码',
  `realm` varchar(32) DEFAULT NULL COMMENT '登录域',
  `actor_type` varchar(32) DEFAULT NULL COMMENT '操作者类型',
  `party_type` varchar(64) DEFAULT NULL COMMENT '归属主体类型',
  `party_id` bigint DEFAULT NULL COMMENT '归属主体ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_subject_role_subject_role` (`subject_type`,`subject_id`,`role_id`,`tenant_id`,`app_code`,`party_type`,`party_id`),
  KEY `idx_authorization_subject_role_role_id` (`role_id`),
  KEY `idx_authorization_subject_role_tenant_id` (`tenant_id`),
  KEY `idx_authorization_subject_role_context` (`subject_type`,`subject_id`,`app_code`,`realm`,`party_type`,`party_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='主体角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_subject_role` WRITE;
/*!40000 ALTER TABLE `authorization_subject_role` DISABLE KEYS */;
INSERT INTO `authorization_subject_role` (`id`, `tenant_id`, `subject_id`, `subject_type`, `app_code`, `realm`, `actor_type`, `party_type`, `party_id`, `role_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,1,1001,'TENANT_MEMBER','internal-admin','INTERNAL','INTERNAL_USER','INTERNAL_ORG',1,1,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(2,2,1002,'TENANT_MEMBER','internal-admin','INTERNAL','INTERNAL_USER','INTERNAL_ORG',2,2,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(3,3,1003,'TENANT_MEMBER','internal-admin','INTERNAL','INTERNAL_USER','INTERNAL_ORG',3,3,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(4,4,1004,'TENANT_MEMBER','internal-admin','INTERNAL','INTERNAL_USER','INTERNAL_ORG',4,4,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23');
/*!40000 ALTER TABLE `authorization_subject_role` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_role_permission` (
  `id` bigint NOT NULL COMMENT '主键',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `perm_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_role_permission_role_perm` (`role_id`,`perm_id`),
  KEY `idx_authorization_role_permission_role_id` (`role_id`),
  KEY `idx_authorization_role_permission_perm_id` (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_role_permission` WRITE;
/*!40000 ALTER TABLE `authorization_role_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `authorization_role_permission` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_subject_permission` (
  `id` bigint NOT NULL COMMENT '主键',
  `subject_id` bigint NOT NULL COMMENT '主体ID',
  `perm_id` bigint NOT NULL COMMENT '权限ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_subject_permission_subject_perm` (`subject_id`,`perm_id`,`tenant_id`),
  KEY `idx_authorization_subject_permission_subject_id` (`subject_id`),
  KEY `idx_authorization_subject_permission_perm_id` (`perm_id`),
  KEY `idx_authorization_subject_permission_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='主体直授权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_subject_permission` WRITE;
/*!40000 ALTER TABLE `authorization_subject_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `authorization_subject_permission` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_app` (
  `id` bigint NOT NULL COMMENT '主键',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码',
  `app_name` varchar(100) NOT NULL COMMENT '应用名称',
  `realm` varchar(32) DEFAULT NULL COMMENT '已迁移至 authorization_app_login_context.realm',
  `actor_type` varchar(32) DEFAULT NULL COMMENT '已迁移至 authorization_app_login_context.actor_type',
  `icon` varchar(64) DEFAULT NULL COMMENT '应用图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_app_code` (`app_code`),
  KEY `idx_authorization_app_realm` (`realm`),
  KEY `idx_authorization_app_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='授权应用入口表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_app` WRITE;
/*!40000 ALTER TABLE `authorization_app` DISABLE KEYS */;
INSERT INTO `authorization_app` (`id`, `app_code`, `app_name`, `realm`, `actor_type`, `icon`, `sort`, `status`, `remark`, `create_time`, `update_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `tenant_id`) VALUES (1,'internal-admin','内部管理后台','INTERNAL','INTERNAL_USER','Setting',1,1,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23','default');
/*!40000 ALTER TABLE `authorization_app` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_app_login_context` (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码',
  `realm` varchar(32) NOT NULL COMMENT '登录域',
  `actor_type` varchar(32) NOT NULL COMMENT '操作者类型',
  `default_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认上下文: 0-否, 1-是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_app_context` (`app_code`,`realm`,`actor_type`),
  KEY `idx_authorization_app_context_app_id` (`app_id`),
  KEY `idx_authorization_app_context_realm` (`realm`),
  KEY `idx_authorization_app_context_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='授权应用登录上下文表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_app_login_context` WRITE;
/*!40000 ALTER TABLE `authorization_app_login_context` DISABLE KEYS */;
INSERT INTO `authorization_app_login_context` (`id`, `app_id`, `app_code`, `realm`, `actor_type`, `default_flag`, `status`, `sort`, `create_time`, `update_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `tenant_id`) VALUES (1,1,'internal-admin','INTERNAL','INTERNAL_USER',1,1,0,'2026-05-10 00:04:23','2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23','default');
/*!40000 ALTER TABLE `authorization_app_login_context` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_menu` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `app_code` varchar(64) NOT NULL DEFAULT 'internal-admin' COMMENT '应用编码',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父菜单ID',
  `menu_type` tinyint NOT NULL DEFAULT '1' COMMENT '菜单类型: 1-目录, 2-菜单, 3-按钮',
  `menu_name` varchar(64) NOT NULL COMMENT '菜单名称',
  `menu_code` varchar(128) DEFAULT NULL COMMENT '菜单权限标识',
  `path` varchar(255) DEFAULT NULL COMMENT '前端路由路径',
  `icon` varchar(64) DEFAULT NULL COMMENT '菜单图标',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否显示: 0-隐藏, 1-显示',
  `keep_alive` tinyint NOT NULL DEFAULT '0' COMMENT '路由缓存: 0-不缓存, 1-缓存',
  `embedded` tinyint NOT NULL DEFAULT '0' COMMENT '内嵌模式: 0-否, 1-iframe内嵌',
  `redirect` varchar(255) DEFAULT NULL COMMENT '重定向路径',
  `permissions` varchar(500) DEFAULT NULL COMMENT '权限标识列表',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记: 0-正常, 1-已删除',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_authorization_menu_parent_id` (`parent_id`),
  KEY `idx_authorization_menu_app_parent` (`app_code`,`parent_id`),
  KEY `idx_authorization_menu_type` (`menu_type`),
  KEY `idx_authorization_menu_status` (`status`),
  KEY `idx_authorization_menu_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_menu` WRITE;
/*!40000 ALTER TABLE `authorization_menu` DISABLE KEYS */;
INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,1,'internal-admin',0,1,'系统管理','system','/system','Setting',NULL,1,1,1,0,0,'/system/permission',NULL,NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','顶部系统入口',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(2,1,'internal-admin',1,1,'账号权限','system:account-access','/system/account-access','UserFilled',NULL,1,1,1,0,0,'/system/role',NULL,NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','用户、角色和授权管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(3,1,'internal-admin',2,2,'角色管理','system:role','/system/role','UserFilled','@/views/system/role/index.vue',2,1,1,0,0,NULL,'system:role:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','角色管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(4,1,'internal-admin',19,2,'菜单管理','system:menu','/system/menu','Menu','@/views/system/menu/index.vue',3,1,1,0,0,NULL,'system:menu:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','菜单管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(5,1,'internal-admin',1,1,'基础数据','system:base-data','/system/base-data','DataBoard',NULL,4,1,1,0,0,'/system/dict',NULL,NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','字典、参数、行政区划等基础数据',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(6,1,'internal-admin',5,2,'参数配置','system:config','/system/config','Setting','@/views/system/config/index.vue',2,1,1,0,0,NULL,'system:config:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','系统参数配置',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(7,1,'internal-admin',5,2,'字典管理','system:dict','/system/dict','Collection','@/views/system/dict/index.vue',1,1,1,0,0,NULL,'system:dict:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','字典类型与字典数据管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(8,1,'internal-admin',1,1,'审计日志','system:audit-log','/system/audit-log','DocumentChecked',NULL,5,1,1,0,0,'/system/login-log',NULL,NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','登录与操作审计日志',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(9,1,'internal-admin',8,2,'登录日志','system:log:login','/system/login-log','DocumentChecked','@/views/system/login-log/index.vue',1,1,1,0,0,NULL,'system:log:login:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','登录日志页面入口',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10,1,'internal-admin',8,2,'操作日志','system:log:operation','/system/operation-log','Tickets','@/views/system/operation-log/index.vue',2,1,1,0,0,NULL,'system:log:operation:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','操作日志页面入口',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(11,1,'internal-admin',1,1,'租户与组织','system:tenant-org','/system/tenant-org','OfficeBuilding',NULL,4,0,0,0,0,'/system/tenant',NULL,NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','已由平台运营目录承接，保留历史 ID 但不再展示',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(12,1,'internal-admin',19,2,'机构管理','system:tenant','/system/tenant','OfficeBuilding','@/views/system/tenant/index.vue',1,1,1,0,0,NULL,'system:tenant:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(13,1,'internal-admin',1,1,'平台应用','system:platform','/system/platform','Box',NULL,5,0,0,0,0,'/system/app',NULL,NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','已由平台运营目录承接，保留历史 ID 但不再展示',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(14,1,'internal-admin',19,2,'应用管理','system:app','/system/app','Box','@/views/system/app/index.vue',2,1,1,0,0,NULL,'authorization:app:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','授权应用入口管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(15,1,'internal-admin',18,2,'岗位管理','system:post','/system/post','Postcard','@/views/system/post/index.vue',2,1,1,0,0,NULL,'system:post:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','岗位基础信息管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(16,1,'internal-admin',5,2,'行政区划','system:area','/system/area','MapLocation','@/views/system/area/index.vue',3,1,1,0,0,NULL,'system:area:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','行政区划逐级管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(17,1,'internal-admin',18,2,'组织架构','system:org','/system/org','OfficeBuilding','@/views/system/org/index.vue',1,1,1,0,0,NULL,'system:org:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','组织架构树与详情查询',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(18,1,'internal-admin',1,1,'组织人事','system:org-hr','/system/org-hr','Connection',NULL,2,1,1,0,0,'/system/org',NULL,NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','租户内组织架构与岗位管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(19,1,'internal-admin',1,1,'平台运营','system:platform-ops','/system/platform-ops','Platform',NULL,3,1,1,0,0,'/system/tenant',NULL,NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','平台级机构、应用和系统元数据管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(20,1,'internal-admin',2,2,'成员管理','system:user','/system/user','User','@/views/system/user/index.vue',1,1,1,0,0,NULL,'system:user:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构成员账号管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(21,1,'internal-admin',5,2,'路由管理','system:route','/system/route','Switch','@/views/system/route/index.vue',4,1,1,0,0,NULL,'system:route:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','平台运行路由配置管理',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(22,1,'internal-admin',19,2,'文件管理','system:file','/system/file','FolderOpened','@/views/system/file/index.vue',6,1,1,0,0,NULL,'system:file:list',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','平台文件记录、上传下载和归档管理',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(23,1,'internal-admin',19,2,'文件存储配置','system:file-storage','/system/file-storage','HardDriveUpload','@/views/system/file-storage/index.vue',7,1,1,0,0,NULL,'system:file-storage:list',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','平台文件第三方存储配置管理',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(1201,1,'internal-admin',12,3,'查询机构','system:tenant:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:tenant:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(1202,1,'internal-admin',12,3,'新增机构','system:tenant:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:tenant:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(1203,1,'internal-admin',12,3,'编辑机构','system:tenant:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:tenant:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构编辑权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(1204,1,'internal-admin',12,3,'删除机构','system:tenant:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:tenant:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构删除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(1401,1,'internal-admin',14,3,'查询应用','authorization:app:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'authorization:app:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','授权应用详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1402,1,'internal-admin',14,3,'新增应用','authorization:app:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'authorization:app:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','授权应用新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1403,1,'internal-admin',14,3,'修改应用','authorization:app:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'authorization:app:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','授权应用修改权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1404,1,'internal-admin',14,3,'删除应用','authorization:app:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'authorization:app:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','授权应用删除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1500,1,'internal-admin',15,3,'查询岗位列表','system:post:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:post:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','岗位列表查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1501,1,'internal-admin',15,3,'查询岗位','system:post:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:post:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','岗位详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1502,1,'internal-admin',15,3,'新增岗位','system:post:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:post:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','岗位新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1503,1,'internal-admin',15,3,'修改岗位','system:post:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:post:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','岗位修改权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1504,1,'internal-admin',15,3,'删除岗位','system:post:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:post:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','岗位删除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1601,1,'internal-admin',16,3,'查询行政区划','system:area:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:area:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','行政区划详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1602,1,'internal-admin',16,3,'新增行政区划','system:area:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:area:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','行政区划新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1603,1,'internal-admin',16,3,'修改行政区划','system:area:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:area:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','行政区划修改权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1604,1,'internal-admin',16,3,'删除行政区划','system:area:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:area:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','行政区划删除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1700,1,'internal-admin',17,3,'查询组织列表','system:org:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:org:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','组织列表查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1701,1,'internal-admin',17,3,'查询组织','system:org:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:org:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','组织详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1702,1,'internal-admin',17,3,'新增组织','system:org:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:org:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','组织新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1703,1,'internal-admin',17,3,'修改组织','system:org:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:org:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','组织修改权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1704,1,'internal-admin',17,3,'删除组织','system:org:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:org:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','组织删除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(2000,1,'internal-admin',20,3,'查询成员','system:user:list',NULL,NULL,NULL,0,1,0,0,0,NULL,'system:user:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构成员查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(2001,1,'internal-admin',20,3,'查询成员','system:user:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:user:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构成员详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(2002,1,'internal-admin',20,3,'新增成员','system:user:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:user:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构成员新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(2003,1,'internal-admin',20,3,'编辑成员','system:user:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:user:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构成员编辑权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(2004,1,'internal-admin',20,3,'移除成员','system:user:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:user:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构成员移除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(2005,1,'internal-admin',20,3,'调整成员状态','system:user:status',NULL,NULL,NULL,5,1,0,0,0,NULL,'system:user:status',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构成员状态调整权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(2006,1,'internal-admin',20,3,'重置成员密码','system:user:reset-password',NULL,NULL,NULL,6,1,0,0,0,NULL,'system:user:reset-password',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构成员密码重置权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(2007,1,'internal-admin',20,3,'分配成员角色','system:user:assign-role',NULL,NULL,NULL,7,1,0,0,0,NULL,'authorization:role:assign',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:24','机构成员角色分配权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:24'),(3000,1,'internal-admin',3,3,'查询角色列表','authorization:role:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'authorization:role:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','角色列表查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(3001,1,'internal-admin',3,3,'查询角色','authorization:role:query',NULL,NULL,NULL,2,1,0,0,0,NULL,'authorization:role:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','角色详情与角色授权查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(3002,1,'internal-admin',3,3,'新增角色','authorization:role:add',NULL,NULL,NULL,3,1,0,0,0,NULL,'authorization:role:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','角色新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(3003,1,'internal-admin',3,3,'修改角色','authorization:role:edit',NULL,NULL,NULL,4,1,0,0,0,NULL,'authorization:role:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','角色修改权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(3004,1,'internal-admin',3,3,'删除角色','authorization:role:delete',NULL,NULL,NULL,5,1,0,0,0,NULL,'authorization:role:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','角色删除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(3005,1,'internal-admin',3,3,'分配角色权限','authorization:role:assign',NULL,NULL,NULL,6,1,0,0,0,NULL,'authorization:role:assign',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','角色菜单与主体角色分配权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(4001,1,'internal-admin',4,3,'查询菜单','system:menu:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:menu:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','菜单详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(4002,1,'internal-admin',4,3,'新增菜单','system:menu:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:menu:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','菜单新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(4003,1,'internal-admin',4,3,'修改菜单','system:menu:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:menu:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','菜单修改权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(4004,1,'internal-admin',4,3,'删除菜单','system:menu:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:menu:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','菜单删除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(6001,1,'internal-admin',6,3,'查询系统配置','system:config:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:config:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','系统配置详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(6002,1,'internal-admin',6,3,'新增系统配置','system:config:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:config:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','系统配置新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(6003,1,'internal-admin',6,3,'修改系统配置','system:config:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:config:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','系统配置修改权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(6004,1,'internal-admin',6,3,'删除系统配置','system:config:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:config:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','系统配置删除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(9001,1,'internal-admin',1,3,'全部权限','*:*',NULL,NULL,NULL,999,1,0,0,0,NULL,NULL,NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23',NULL,0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(9002,1,'internal-admin',9,3,'查询登录日志列表','system:log:login:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:log:login:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','登录日志列表查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(9003,1,'internal-admin',9,3,'查询登录日志','system:log:login:query',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:log:login:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','登录日志详情与导出查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(9004,1,'internal-admin',9,3,'清理登录日志','system:log:login:delete',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:log:login:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','登录日志清理权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10002,1,'internal-admin',10,3,'查询操作日志列表','system:log:operation:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:log:operation:list',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','操作日志列表查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10003,1,'internal-admin',10,3,'查询操作日志','system:log:operation:query',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:log:operation:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','操作日志详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10004,1,'internal-admin',10,3,'清理操作日志','system:log:operation:delete',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:log:operation:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','操作日志清理权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(21001,1,'internal-admin',21,3,'查询系统路由','system:route:query',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:route:query',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','系统路由详情查询权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(21002,1,'internal-admin',21,3,'新增系统路由','system:route:add',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:route:add',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','系统路由新增权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(21003,1,'internal-admin',21,3,'修改系统路由','system:route:edit',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:route:edit',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','系统路由修改和排序权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(21004,1,'internal-admin',21,3,'删除系统路由','system:route:delete',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:route:delete',NULL,NULL,'2026-05-10 00:04:23','2026-05-10 00:04:23','系统路由删除权限',0,NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(22001,1,'internal-admin',22,3,'查询文件列表','system:file:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:file:list',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件记录列表查询权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(22002,1,'internal-admin',22,3,'查询文件详情','system:file:query',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:file:query',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件详情与预览元数据查询权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(22003,1,'internal-admin',22,3,'上传文件','system:file:upload',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:file:upload',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件上传权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(22004,1,'internal-admin',22,3,'下载文件','system:file:download',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:file:download',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件下载权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(22005,1,'internal-admin',22,3,'归档文件','system:file:archive',NULL,NULL,NULL,5,1,0,0,0,NULL,'system:file:archive',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件归档权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(23001,1,'internal-admin',23,3,'查询存储配置列表','system:file-storage:list',NULL,NULL,NULL,1,1,0,0,0,NULL,'system:file-storage:list',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件存储配置列表查询权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(23002,1,'internal-admin',23,3,'查询存储配置详情','system:file-storage:query',NULL,NULL,NULL,2,1,0,0,0,NULL,'system:file-storage:query',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件存储配置详情查询权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(23003,1,'internal-admin',23,3,'新增存储配置','system:file-storage:add',NULL,NULL,NULL,3,1,0,0,0,NULL,'system:file-storage:add',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件存储配置新增权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(23004,1,'internal-admin',23,3,'修改存储配置','system:file-storage:edit',NULL,NULL,NULL,4,1,0,0,0,NULL,'system:file-storage:edit',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件存储配置修改权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(23005,1,'internal-admin',23,3,'删除存储配置','system:file-storage:delete',NULL,NULL,NULL,5,1,0,0,0,NULL,'system:file-storage:delete',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件存储配置删除权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(23006,1,'internal-admin',23,3,'测试存储配置','system:file-storage:test',NULL,NULL,NULL,6,1,0,0,0,NULL,'system:file-storage:test',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件存储配置连接测试权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(23007,1,'internal-admin',23,3,'启用默认存储配置','system:file-storage:active',NULL,NULL,NULL,7,1,0,0,0,NULL,'system:file-storage:active',NULL,NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24','文件存储配置启用权限',0,NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24');
/*!40000 ALTER TABLE `authorization_menu` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `authorization_role_menu` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_authorization_role_menu_role_id` (`role_id`),
  KEY `idx_authorization_role_menu_menu_id` (`menu_id`),
  KEY `idx_authorization_role_menu_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `authorization_role_menu` WRITE;
/*!40000 ALTER TABLE `authorization_role_menu` DISABLE KEYS */;
INSERT INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (9001,1,1,9001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10002,1,1,2,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10003,1,1,3,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10004,1,1,4,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10005,1,1,5,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10006,1,1,6,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10007,1,1,7,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10008,1,1,8,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10009,1,1,9,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10010,1,1,10,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10011,1,1,11,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10012,1,1,12,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10013,1,1,13,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10014,1,1,14,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10015,1,1,15,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10016,1,1,16,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10017,1,1,17,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10018,1,1,18,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10019,1,1,19,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(11201,1,1,1201,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(11202,1,1,1202,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(11203,1,1,1203,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(11204,1,1,1204,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(11401,1,1,1401,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(11402,1,1,1402,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(11403,1,1,1403,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(11404,1,1,1404,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(200001,2,2,1,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(200002,2,2,2,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(200003,2,2,3,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(200008,2,2,8,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(200009,2,2,9,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(200010,2,2,10,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(200015,2,2,15,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(200017,2,2,17,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(200018,2,2,18,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(201500,2,2,1500,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(201501,2,2,1501,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(201502,2,2,1502,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(201503,2,2,1503,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(201504,2,2,1504,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(201700,2,2,1700,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(201701,2,2,1701,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(202000,2,2,2000,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(202001,2,2,2001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(202002,2,2,2002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(202003,2,2,2003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(202004,2,2,2004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(202005,2,2,2005,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(202006,2,2,2006,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(202007,2,2,2007,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(203000,2,2,3000,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(203001,2,2,3001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(203002,2,2,3002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(203003,2,2,3003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(203004,2,2,3004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(203005,2,2,3005,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(209002,2,2,9002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(209003,2,2,9003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(210002,2,2,10002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(210003,2,2,10003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(300001,3,3,1,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(300002,3,3,2,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(300003,3,3,3,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(300008,3,3,8,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(300009,3,3,9,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(300010,3,3,10,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(300015,3,3,15,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(300017,3,3,17,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(300018,3,3,18,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(301500,3,3,1500,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(301501,3,3,1501,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(301502,3,3,1502,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(301503,3,3,1503,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(301504,3,3,1504,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(301700,3,3,1700,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(301701,3,3,1701,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(302000,3,3,2000,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(302001,3,3,2001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(302002,3,3,2002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(302003,3,3,2003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(302004,3,3,2004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(302005,3,3,2005,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(302006,3,3,2006,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(302007,3,3,2007,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(303000,3,3,3000,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(303001,3,3,3001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(303002,3,3,3002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(303003,3,3,3003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(303004,3,3,3004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(303005,3,3,3005,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(309002,3,3,9002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(309003,3,3,9003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(310002,3,3,10002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(310003,3,3,10003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(400001,4,4,1,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(400002,4,4,2,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(400003,4,4,3,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(400008,4,4,8,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(400009,4,4,9,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(400010,4,4,10,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(400015,4,4,15,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(400017,4,4,17,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(400018,4,4,18,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(401500,4,4,1500,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(401501,4,4,1501,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(401502,4,4,1502,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(401503,4,4,1503,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(401504,4,4,1504,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(401700,4,4,1700,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(401701,4,4,1701,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(402000,4,4,2000,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(402001,4,4,2001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(402002,4,4,2002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(402003,4,4,2003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(402004,4,4,2004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(402005,4,4,2005,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(402006,4,4,2006,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(402007,4,4,2007,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(403000,4,4,3000,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(403001,4,4,3001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(403002,4,4,3002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(403003,4,4,3003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(403004,4,4,3004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(403005,4,4,3005,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(409002,4,4,9002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(409003,4,4,9003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(410002,4,4,10002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(410003,4,4,10003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1000020,1,1,20,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1000021,1,1,21,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001500,1,1,1500,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001501,1,1,1501,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001502,1,1,1502,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001503,1,1,1503,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001504,1,1,1504,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001601,1,1,1601,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001602,1,1,1602,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001603,1,1,1603,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001604,1,1,1604,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001700,1,1,1700,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1001701,1,1,1701,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1002000,1,1,2000,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1002001,1,1,2001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1002002,1,1,2002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1002003,1,1,2003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1002004,1,1,2004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1002005,1,1,2005,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1002006,1,1,2006,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1002007,1,1,2007,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1003000,1,1,3000,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1003001,1,1,3001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1003002,1,1,3002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1003003,1,1,3003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1003004,1,1,3004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1003005,1,1,3005,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1004001,1,1,4001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1004002,1,1,4002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1004003,1,1,4003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1004004,1,1,4004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1006001,1,1,6001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1006002,1,1,6002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1006003,1,1,6003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1006004,1,1,6004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1009002,1,1,9002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1009003,1,1,9003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1010002,1,1,10002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1010003,1,1,10003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1021001,1,1,21001,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1021002,1,1,21002,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1021003,1,1,21003,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1021004,1,1,21004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(10022000,1,1,22,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(10022001,1,1,22001,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(10022002,1,1,22002,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(10022003,1,1,22003,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(10022004,1,1,22004,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(10022005,1,1,22005,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(100000023,1,1,23,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(100023001,1,1,23001,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(100023002,1,1,23002,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(100023003,1,1,23003,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(100023004,1,1,23004,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(100023005,1,1,23005,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(100023006,1,1,23006,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(100023007,1,1,23007,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24'),(37334242082386897,1,1,1702,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(53351364904228191,2,2,1704,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(55962888505413893,4,4,1704,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(62332016171651708,4,4,1703,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(69030248003263524,1,1,10004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(126542466256546369,1,1,1703,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(153395634361364297,3,3,1704,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(212210984645665930,4,4,10004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(278450800667223653,2,2,10004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(295901035610495542,3,3,1702,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(310503183398326360,2,2,1702,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(347768834693630425,4,4,9004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(435347288727686965,2,2,9004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(486930089851302188,4,4,1702,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(494724266233590872,4,4,20,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(514943748960680726,3,3,10004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(516194977456884538,1,1,9004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(516682634025375270,2,2,1703,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(820721515595595161,2,2,20,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(902407985808418383,3,3,9004,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1056780791580299046,3,3,1703,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1057096571897137168,3,3,20,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23'),(1059572375804610387,1,1,1704,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23',NULL,'2026-05-10 00:04:23');
/*!40000 ALTER TABLE `authorization_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

-- 路由管理与菜单管理职责重复，动态路由统一由菜单管理维护，不再落默认后台入口。
DELETE FROM `authorization_role_menu` WHERE `menu_id` IN (21, 21001, 21002, 21003, 21004);
DELETE FROM `authorization_menu` WHERE `id` IN (21, 21001, 21002, 21003, 21004);
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
