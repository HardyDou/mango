
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
CREATE TABLE IF NOT EXISTS `file_record` (
  `id` bigint NOT NULL COMMENT '文件ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID，用户可见语义为机构，技术字段保留tenant_id',
  `biz_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `biz_id` varchar(128) DEFAULT NULL COMMENT '业务ID',
  `purpose` varchar(64) DEFAULT NULL COMMENT '文件用途',
  `biz_meta` json DEFAULT NULL COMMENT '业务自定义参数JSON',
  `access_level` varchar(32) NOT NULL DEFAULT 'PRIVATE' COMMENT '访问级别: PRIVATE-机构私有 PUBLIC_READ-公开读取 INTERNAL-内部文件',
  `storage_type` varchar(32) NOT NULL DEFAULT 'LOCAL' COMMENT '存储类型: LOCAL-本地 S3-S3兼容',
  `storage_config_id` bigint DEFAULT NULL COMMENT '存储配置ID',
  `bucket_name` varchar(128) NOT NULL DEFAULT 'local' COMMENT '存储桶名称',
  `object_name` varchar(500) NOT NULL COMMENT '对象名称',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_ext` varchar(32) DEFAULT NULL COMMENT '文件扩展名',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小，单位字节',
  `content_type` varchar(128) DEFAULT NULL COMMENT '内容类型',
  `file_hash` varchar(128) DEFAULT NULL COMMENT '文件哈希',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-上传中 1-完成 2-失败 9-归档',
  `archived` tinyint NOT NULL DEFAULT '0' COMMENT '是否归档: 0-否 1-是',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_object` (`bucket_name`,`object_name`),
  KEY `idx_file_tenant` (`tenant_id`),
  KEY `idx_file_biz` (`biz_type`,`biz_id`),
  KEY `idx_file_purpose` (`purpose`),
  KEY `idx_file_status` (`status`,`archived`),
  KEY `idx_file_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `file_record` WRITE;
/*!40000 ALTER TABLE `file_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_record` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `file_storage_config` (
  `id` bigint NOT NULL COMMENT '存储配置ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '机构隔离ID，用户可见语义为机构，技术字段保留tenant_id',
  `config_name` varchar(64) NOT NULL COMMENT '配置名称',
  `storage_type` varchar(32) NOT NULL COMMENT '存储类型: LOCAL-本地 S3-S3兼容 MINIO-MinIO AWS_S3-AWS S3 ALIYUN_OSS-阿里云OSS TENCENT_COS-腾讯云COS QINIU_KODO-七牛云Kodo',
  `endpoint` varchar(255) DEFAULT NULL COMMENT '接入地址',
  `public_endpoint` varchar(255) DEFAULT NULL COMMENT '公开访问地址',
  `region` varchar(64) DEFAULT NULL COMMENT '区域',
  `bucket_name` varchar(128) NOT NULL COMMENT '存储桶名称',
  `access_key` varchar(255) DEFAULT NULL COMMENT '访问密钥AccessKey',
  `secret_key` varchar(512) DEFAULT NULL COMMENT '访问密钥SecretKey',
  `path_style_access` tinyint NOT NULL DEFAULT '0' COMMENT '是否使用Path Style访问: 0-否 1-是',
  `ssl_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用HTTPS: 0-否 1-是',
  `active` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认启用: 0-否 1-是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用 1-启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_storage_config_name` (`config_name`),
  KEY `idx_file_storage_type` (`storage_type`),
  KEY `idx_file_storage_active` (`active`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件存储配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `file_storage_config` WRITE;
/*!40000 ALTER TABLE `file_storage_config` DISABLE KEYS */;
INSERT INTO `file_storage_config` (`id`, `tenant_id`, `config_name`, `storage_type`, `endpoint`, `public_endpoint`, `region`, `bucket_name`, `access_key`, `secret_key`, `path_style_access`, `ssl_enabled`, `active`, `status`, `remark`, `created_by`, `created_time`, `created_at`, `updated_by`, `updated_time`, `updated_at`) VALUES (1,1,'本地默认存储','LOCAL',NULL,NULL,NULL,'local',NULL,NULL,0,0,1,1,'系统默认本地文件存储',NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24',NULL,'2026-05-10 00:04:24','2026-05-10 00:04:24');
/*!40000 ALTER TABLE `file_storage_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
