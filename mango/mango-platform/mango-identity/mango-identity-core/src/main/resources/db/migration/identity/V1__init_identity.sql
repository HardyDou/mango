
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
CREATE TABLE IF NOT EXISTS `identity_user` (
  `id` bigint NOT NULL COMMENT '主键',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `password` varchar(200) NOT NULL COMMENT '密码哈希',
  `password_reset_required` tinyint NOT NULL DEFAULT '0' COMMENT '是否要求下次登录修改密码',
  `password_updated_at` datetime DEFAULT NULL COMMENT '最近密码更新时间',
  `nickname` varchar(100) DEFAULT NULL COMMENT '昵称',
  `realm` varchar(32) NOT NULL DEFAULT 'INTERNAL' COMMENT '登录域',
  `actor_type` varchar(32) NOT NULL DEFAULT 'INTERNAL_USER' COMMENT '操作者类型',
  `party_type` varchar(64) DEFAULT NULL COMMENT '归属主体类型',
  `party_id` bigint DEFAULT NULL COMMENT '归属主体ID',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
  `failed_login_count` int NOT NULL DEFAULT '0' COMMENT '连续登录失败次数',
  `last_failed_login_at` datetime DEFAULT NULL COMMENT '最近登录失败时间',
  `locked_until` datetime DEFAULT NULL COMMENT '账号锁定截止时间',
  `locked_reason` varchar(200) DEFAULT NULL COMMENT '账号锁定原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',
  `org_id` bigint DEFAULT NULL COMMENT '组织标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_identity_user_realm_username` (`realm`,`username`),
  KEY `idx_identity_user_username` (`username`),
  KEY `idx_identity_user_party` (`party_type`,`party_id`),
  KEY `idx_identity_user_status` (`status`),
  KEY `idx_identity_user_locked_until` (`locked_until`),
  KEY `idx_identity_user_last_failed_login_at` (`last_failed_login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='身份用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `tenant_member` (
  `id` bigint NOT NULL COMMENT '成员ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织标识',
  `user_id` bigint NOT NULL COMMENT '全局账号ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT '成员编号',
  `display_name` varchar(100) NOT NULL COMMENT '成员显示名称',
  `member_type` varchar(32) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '成员类型',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `primary_org_id` bigint DEFAULT NULL COMMENT '主组织ID',
  `primary_post_id` bigint DEFAULT NULL COMMENT '主岗位ID',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `left_at` datetime DEFAULT NULL COMMENT '离开时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_member_tenant_user` (`tenant_id`,`user_id`),
  UNIQUE KEY `uk_tenant_member_tenant_no` (`tenant_id`,`member_no`),
  KEY `idx_tenant_member_user` (`user_id`),
  KEY `idx_tenant_member_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `tenant_member_org` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `member_id` bigint NOT NULL COMMENT '成员ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `post_id` bigint DEFAULT NULL COMMENT '岗位ID',
  `primary_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否主组织岗位: 0-否, 1-是',
  `leader_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否组织主管: 0-否, 1-是',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_member_org_member_org` (`tenant_id`,`member_id`,`org_id`),
  KEY `idx_tenant_member_org_member` (`member_id`),
  KEY `idx_tenant_member_org_org` (`org_id`),
  KEY `idx_tenant_member_org_leader` (`tenant_id`,`org_id`,`leader_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户成员组织岗位关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE TABLE IF NOT EXISTS `identity_external_binding` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织标识',
  `user_id` bigint NOT NULL COMMENT 'Mango用户ID',
  `provider` varchar(32) NOT NULL COMMENT '身份提供方',
  `corp_id` varchar(128) NOT NULL COMMENT '企业ID',
  `external_user_id` varchar(128) NOT NULL COMMENT '外部用户ID',
  `display_name` varchar(128) DEFAULT NULL COMMENT '显示名称快照',
  `bind_source` varchar(32) NOT NULL DEFAULT 'SYNC' COMMENT '绑定来源',
  `bind_status` varchar(32) NOT NULL DEFAULT 'BOUND' COMMENT '绑定状态',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近第三方登录时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_external_binding_external` (`tenant_id`, `provider`, `corp_id`, `external_user_id`),
  KEY `idx_external_binding_user` (`tenant_id`, `user_id`),
  KEY `idx_external_binding_provider` (`provider`, `corp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='第三方登录身份绑定表';
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
