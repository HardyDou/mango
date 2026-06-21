-- Baseline migration for module: authorization
-- Squashed from 15 migration files before first shared release.
-- REBASE_REQUIRED(issue-204): frontend runtime tables were renamed to the authorization_* namespace.
-- Databases that already applied earlier local migrations must be rebuilt from this baseline.

-- -----------------------------------------------------------------------------
-- Squashed from: V1__init_authorization.sql
-- -----------------------------------------------------------------------------

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
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.
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
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.
/*!40000 ALTER TABLE `authorization_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;



-- -----------------------------------------------------------------------------
-- Folded from V2__menu_package_and_navigation_restructure.sql
-- -----------------------------------------------------------------------------

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

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

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

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.



-- -----------------------------------------------------------------------------
-- Folded from V3__workflow_task_menu_component_fix.sql
-- -----------------------------------------------------------------------------

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.



-- -----------------------------------------------------------------------------
-- Folded from V4__remove_workflow_node_definition_menu.sql
-- -----------------------------------------------------------------------------

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.



-- -----------------------------------------------------------------------------
-- Folded from V5__authorization_frontend_app_registry_runtime.sql
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `authorization_frontend_app_registry` (
  `id` bigint NOT NULL COMMENT '前端入口注册ID',
  `app_code` varchar(64) NOT NULL COMMENT '授权应用编码',
  `app_type` varchar(32) NOT NULL DEFAULT 'LOCAL' COMMENT '前端入口类型: LOCAL/MICRO_APP/IFRAME/EXTERNAL_LINK',
  `deploy_mode` varchar(32) NOT NULL DEFAULT 'EMBEDDED' COMMENT '部署模式: EMBEDDED/REMOTE/HYBRID',
  `entry_url` varchar(500) DEFAULT NULL COMMENT '远程入口地址',
  `mount_path` varchar(255) DEFAULT NULL COMMENT '主框架挂载路径',
  `active_rule` varchar(255) DEFAULT NULL COMMENT '激活规则',
  `framework` varchar(64) DEFAULT NULL COMMENT '前端运行框架',
  `version` varchar(64) DEFAULT NULL COMMENT '当前版本',
  `health_check_url` varchar(500) DEFAULT NULL COMMENT '健康检查地址',
  `sandbox_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用沙箱',
  `style_isolation` varchar(32) NOT NULL DEFAULT 'NONE' COMMENT '样式隔离: NONE/SCOPED/SHADOW_DOM/IFRAME',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_frontend_app_registry_app_code` (`app_code`),
  KEY `idx_authorization_frontend_app_registry_type` (`app_type`),
  KEY `idx_authorization_frontend_app_registry_mount_path` (`mount_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端应用入口注册表';

INSERT IGNORE INTO `authorization_frontend_app_registry`
  (`id`, `app_code`, `app_type`, `deploy_mode`, `framework`, `sandbox_enabled`, `style_isolation`, `create_time`, `update_time`)
VALUES
  (1, 'internal-admin', 'LOCAL', 'EMBEDDED', 'vue3', 0, 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE IF NOT EXISTS `frontend_menu_runtime_config` (
  `id` bigint NOT NULL COMMENT '菜单运行配置ID',
  `menu_id` bigint NOT NULL COMMENT '授权菜单ID',
  `app_code` varchar(64) NOT NULL COMMENT '授权应用编码',
  `page_type` varchar(32) NOT NULL DEFAULT 'LOCAL_ROUTE' COMMENT '页面运行类型: LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK/BUTTON',
  `external_url` varchar(500) DEFAULT NULL COMMENT 'iframe 或外链地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_frontend_menu_runtime_menu_id` (`menu_id`),
  KEY `idx_frontend_menu_runtime_app_code` (`app_code`),
  KEY `idx_frontend_menu_runtime_page_type` (`page_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端菜单运行配置表';

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

CREATE TABLE IF NOT EXISTS `frontend_tenant_app_binding` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户 ID',
  `app_code` varchar(64) NOT NULL COMMENT '前端入口所属授权应用编码',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_frontend_tenant_app_binding` (`tenant_id`,`app_code`),
  KEY `idx_frontend_tenant_app_binding_app_code` (`app_code`),
  KEY `idx_frontend_tenant_app_binding_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端租户应用开通关系表';

INSERT IGNORE INTO `frontend_tenant_app_binding`
  (`id`, `tenant_id`, `app_code`, `status`, `create_time`, `update_time`)
SELECT `tenant_id`, `tenant_id`, 'internal-admin', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM `authorization_role`
WHERE `app_code` = 'internal-admin'
GROUP BY `tenant_id`;



-- -----------------------------------------------------------------------------
-- Folded from V6__app_module_menu_resource_pool.sql
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `authorization_app_module` (
  `id` bigint NOT NULL COMMENT '主键',
  `app_code` varchar(64) NOT NULL COMMENT '逻辑应用编码',
  `module_code` varchar(128) NOT NULL COMMENT '能力模块编码',
  `module_name` varchar(128) NOT NULL COMMENT '能力模块名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_app_module` (`app_code`,`module_code`),
  KEY `idx_authorization_app_module_app` (`app_code`),
  KEY `idx_authorization_app_module_module` (`module_code`),
  KEY `idx_authorization_app_module_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='逻辑应用集成能力模块表';

SET @menu_module_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'authorization_menu'
    AND COLUMN_NAME = 'module_code'
);
SET @add_menu_module_column_sql := IF(
  @menu_module_column_exists = 0,
  'ALTER TABLE `authorization_menu` ADD COLUMN `module_code` varchar(128) NOT NULL DEFAULT ''mango-system'' COMMENT ''能力模块编码'' AFTER `app_code`',
  'SELECT 1'
);
PREPARE add_menu_module_column_stmt FROM @add_menu_module_column_sql;
EXECUTE add_menu_module_column_stmt;
DEALLOCATE PREPARE add_menu_module_column_stmt;

SET @menu_module_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'authorization_menu'
    AND INDEX_NAME = 'idx_authorization_menu_module_code'
);
SET @add_menu_module_index_sql := IF(
  @menu_module_index_exists = 0,
  'ALTER TABLE `authorization_menu` ADD KEY `idx_authorization_menu_module_code` (`module_code`)',
  'SELECT 1'
);
PREPARE add_menu_module_index_stmt FROM @add_menu_module_index_sql;
EXECUTE add_menu_module_index_stmt;
DEALLOCATE PREPARE add_menu_module_index_stmt;

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (1, 'internal-admin', 'mango-authorization', 'mango-authorization', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'internal-admin', 'mango-system', 'mango-system', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'internal-admin', 'mango-workflow', '审批中心模块', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.



-- -----------------------------------------------------------------------------
-- Folded from V7__authorization_frontend_module_runtime_strategy.sql
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `authorization_frontend_module_runtime_strategy` (
  `id` bigint NOT NULL COMMENT '模块运行策略ID',
  `app_code` varchar(64) NOT NULL COMMENT '逻辑应用编码',
  `module_code` varchar(128) NOT NULL COMMENT '能力模块编码',
  `deploy_profile` varchar(32) NOT NULL DEFAULT 'monolith' COMMENT '部署配置档: monolith/hybrid/micro',
  `page_type` varchar(32) NOT NULL DEFAULT 'LOCAL_ROUTE' COMMENT '页面运行类型: LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK',
  `runtime_code` varchar(64) NOT NULL COMMENT '前端运行单元编码，关联 authorization_frontend_app_registry.app_code',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authorization_frontend_module_runtime_strategy` (`app_code`,`module_code`,`deploy_profile`),
  KEY `idx_authorization_frontend_module_runtime_strategy_app` (`app_code`),
  KEY `idx_authorization_frontend_module_runtime_strategy_runtime` (`runtime_code`),
  KEY `idx_authorization_frontend_module_runtime_strategy_profile` (`deploy_profile`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端模块运行策略表';

INSERT INTO `authorization_frontend_app_registry`
  (`id`, `app_code`, `app_type`, `deploy_mode`, `entry_url`, `mount_path`, `active_rule`, `framework`, `version`, `sandbox_enabled`, `style_isolation`, `create_time`, `update_time`)
VALUES
  (1001, 'mango-admin-local', 'LOCAL', 'EMBEDDED', NULL, '/', '/**', 'vue3', 'dev', 0, 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1002, 'mango-admin-rbac-app', 'MICRO_APP', 'REMOTE', 'http://127.0.0.1:5181/src/micro.ts', '/micro/rbac', '/system/**', 'vue3', 'dev', 0, 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `app_type` = VALUES(`app_type`),
  `deploy_mode` = VALUES(`deploy_mode`),
  `entry_url` = VALUES(`entry_url`),
  `mount_path` = VALUES(`mount_path`),
  `active_rule` = VALUES(`active_rule`),
  `framework` = VALUES(`framework`),
  `version` = VALUES(`version`),
  `sandbox_enabled` = VALUES(`sandbox_enabled`),
  `style_isolation` = VALUES(`style_isolation`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (1, 'internal-admin', 'mango-authorization', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'internal-admin', 'mango-system', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'internal-admin', 'mango-workflow', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (11, 'internal-admin', 'mango-authorization', 'hybrid', 'MICRO_ROUTE', 'mango-admin-rbac-app', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (12, 'internal-admin', 'mango-system', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (13, 'internal-admin', 'mango-workflow', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (21, 'internal-admin', 'mango-authorization', 'micro', 'MICRO_ROUTE', 'mango-admin-rbac-app', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (22, 'internal-admin', 'mango-system', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (23, 'internal-admin', 'mango-workflow', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;



-- -----------------------------------------------------------------------------
-- Folded from workflow approval center menu naming
-- -----------------------------------------------------------------------------

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.


-- -----------------------------------------------------------------------------
-- Folded from V8__workflow_menu_icons.sql
-- -----------------------------------------------------------------------------

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.



-- -----------------------------------------------------------------------------
-- Folded from V9__workflow_business_example_label.sql
-- -----------------------------------------------------------------------------

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.


-- -----------------------------------------------------------------------------
-- Folded from V3__workflow_template_menu.sql
-- -----------------------------------------------------------------------------

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.


-- -----------------------------------------------------------------------------
-- Folded from V4__workflow_template_push_permission.sql
-- -----------------------------------------------------------------------------

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V6__template_center_menu.sql
-- -----------------------------------------------------------------------------
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V7__template_menu_order.sql
-- -----------------------------------------------------------------------------
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V8__template_menu_restructure.sql
-- -----------------------------------------------------------------------------
INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (4, 'internal-admin', 'mango-template', '模板中心模块', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V9__template_menu_authorization_fix.sql
-- -----------------------------------------------------------------------------
SET @template_module_id := (
  SELECT `id`
  FROM `authorization_app_module`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-template'
  LIMIT 1
);

SET @template_module_id := COALESCE(@template_module_id, (
  SELECT COALESCE(MAX(`id`), 0) + 1
  FROM `authorization_app_module`
));

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (@template_module_id, 'internal-admin', 'mango-template', '模板中心模块', 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V10__platform_capability_menu.sql
-- -----------------------------------------------------------------------------
INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (4, 'internal-admin', 'mango-calendar', '工作日历模块', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `module_code` = VALUES(`module_code`),
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (4, 'internal-admin', 'mango-calendar', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (14, 'internal-admin', 'mango-calendar', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (24, 'internal-admin', 'mango-calendar', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V13__calendar_admin_permissions.sql
-- -----------------------------------------------------------------------------
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V14__numgen_capability_menu.sql
-- -----------------------------------------------------------------------------
INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (5, 'internal-admin', 'mango-numgen', '编号生成模块', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `module_code` = VALUES(`module_code`),
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (5, 'internal-admin', 'mango-numgen', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (15, 'internal-admin', 'mango-numgen', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (25, 'internal-admin', 'mango-numgen', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V15__rename_numgen_menu_to_number_rule.sql
-- -----------------------------------------------------------------------------
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V16__restore_template_app_module.sql
-- -----------------------------------------------------------------------------
SET @template_module_id := (
  SELECT `id`
  FROM `authorization_app_module`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-template'
  LIMIT 1
);

SET @template_module_id := COALESCE(@template_module_id, (
  SELECT COALESCE(MAX(`id`), 0) + 1
  FROM `authorization_app_module`
));

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (@template_module_id, 'internal-admin', 'mango-template', '模板管理模块', 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Baseline cleanup: remove duplicated button records introduced by pre-release
-- migration squashing while keeping the canonical menu entry for each feature.
-- -----------------------------------------------------------------------------
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V17__restore_workflow_template_menu_module.sql
-- -----------------------------------------------------------------------------
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V18__platform_capability_menu_restructure.sql
-- -----------------------------------------------------------------------------
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

SET @file_module_id := (
  SELECT `id`
  FROM `authorization_app_module`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-file'
  LIMIT 1
);

SET @file_module_id := COALESCE(@file_module_id, (
  SELECT COALESCE(MAX(`id`), 0) + 1
  FROM `authorization_app_module`
));

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (@file_module_id, 'internal-admin', 'mango-file', '文件管理模块', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (6, 'internal-admin', 'mango-file', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (16, 'internal-admin', 'mango-file', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (26, 'internal-admin', 'mango-file', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

UPDATE `authorization_app_module`
SET `module_name` = '模板管理模块',
    `update_time` = CURRENT_TIMESTAMP
WHERE `app_code` = 'internal-admin'
  AND `module_code` = 'mango-template';

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V19__fix_role_menu_tenant_consistency.sql
-- -----------------------------------------------------------------------------
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V20__remove_workflow_manage_from_tenant_package.sql
-- -----------------------------------------------------------------------------
-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.

-- -----------------------------------------------------------------------------
-- Squashed from: V22__restore_template_module_after_platform_menu_restructure.sql
-- -----------------------------------------------------------------------------
SET @template_module_id := (
  SELECT `id`
  FROM `authorization_app_module`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-template'
  LIMIT 1
);

SET @template_module_id := COALESCE(@template_module_id, (
  SELECT COALESCE(MAX(`id`), 0) + 1
  FROM `authorization_app_module`
));

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (@template_module_id, 'internal-admin', 'mango-template', '模板管理模块', 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

-- Menu data moved to Resource Registry AUTH_MENU; Flyway keeps DDL and non-menu seed data only.
