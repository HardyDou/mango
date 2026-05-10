
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
CREATE TABLE IF NOT EXISTS `guarantee_module_marker` (
  `id` bigint NOT NULL COMMENT '主键',
  `module_name` varchar(64) NOT NULL COMMENT '模块名称',
  `module_stage` varchar(64) NOT NULL COMMENT '模块阶段',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(64) NOT NULL DEFAULT '1' COMMENT '机构标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guarantee_module_marker_name` (`module_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保函协同模块迁移标记表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `guarantee_module_marker` WRITE;
/*!40000 ALTER TABLE `guarantee_module_marker` DISABLE KEYS */;
INSERT INTO `guarantee_module_marker` (`id`, `module_name`, `module_stage`, `created_by`, `created_at`, `updated_by`, `updated_at`, `tenant_id`) VALUES (1,'mango-guarantee','collaboration-foundation',NULL,'2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24','1');
/*!40000 ALTER TABLE `guarantee_module_marker` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `guarantee_case` (
  `id` bigint NOT NULL COMMENT '业务单ID',
  `case_no` varchar(64) NOT NULL COMMENT '业务单编号',
  `source_tenant_id` bigint NOT NULL COMMENT '来源机构ID',
  `title` varchar(200) NOT NULL COMMENT '业务单标题',
  `applicant_name` varchar(100) NOT NULL COMMENT '申请人名称',
  `beneficiary_name` varchar(100) DEFAULT NULL COMMENT '受益人名称',
  `guarantee_type` varchar(64) DEFAULT NULL COMMENT '保函类型编码',
  `amount` decimal(18,2) NOT NULL COMMENT '保函金额',
  `currency` varchar(16) NOT NULL DEFAULT 'CNY' COMMENT '币种编码',
  `expected_issue_date` date DEFAULT NULL COMMENT '期望出函日期',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-草稿，1-处理中，2-已完成，9-已取消',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID，与来源机构一致',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '标准创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '标准更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guarantee_case_no` (`case_no`),
  KEY `idx_guarantee_case_source_tenant` (`source_tenant_id`),
  KEY `idx_guarantee_case_tenant_status` (`tenant_id`,`status`),
  KEY `idx_guarantee_case_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保函业务单';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `guarantee_case` WRITE;
/*!40000 ALTER TABLE `guarantee_case` DISABLE KEYS */;
/*!40000 ALTER TABLE `guarantee_case` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `guarantee_case_participant` (
  `id` bigint NOT NULL COMMENT '参与方ID',
  `case_id` bigint NOT NULL COMMENT '业务单ID',
  `participant_tenant_id` bigint NOT NULL COMMENT '参与机构ID',
  `participant_type` varchar(64) NOT NULL COMMENT '参与方类型',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint NOT NULL COMMENT '记录创建机构ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '标准创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '标准更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guarantee_case_participant` (`case_id`,`participant_tenant_id`,`participant_type`,`del_flag`),
  KEY `idx_guarantee_case_participant_tenant` (`participant_tenant_id`,`status`),
  KEY `idx_guarantee_case_participant_case` (`case_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保函业务参与方';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `guarantee_case_participant` WRITE;
/*!40000 ALTER TABLE `guarantee_case_participant` DISABLE KEYS */;
/*!40000 ALTER TABLE `guarantee_case_participant` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
