-- Baseline migration for module: file
-- Squashed from 6 migration files before first shared release.

-- -----------------------------------------------------------------------------
-- Squashed from: V1__init_file.sql
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
CREATE TABLE IF NOT EXISTS `file_record` (
  `id` bigint NOT NULL COMMENT '文件ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID，用户可见语义为机构，技术字段保留tenant_id',
  `biz_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `biz_id` varchar(128) DEFAULT NULL COMMENT '业务ID',
  `purpose` varchar(64) DEFAULT NULL COMMENT '文件用途',
  `biz_meta` json DEFAULT NULL COMMENT '业务自定义参数JSON',
  `access_level` varchar(32) NOT NULL DEFAULT 'PRIVATE' COMMENT '访问级别: PRIVATE-机构私有 PUBLIC_READ-公开读取 INTERNAL-内部文件',
  `object_id` bigint DEFAULT NULL COMMENT '物理文件对象ID',
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
  KEY `idx_file_record_object_id` (`object_id`),
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



-- -----------------------------------------------------------------------------
-- Folded from V2__file_center_menu.sql
-- -----------------------------------------------------------------------------

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(28,1,'internal-admin',2700,1,'文件管理','file','/file','FolderOpened',NULL,3,1,1,0,0,'/file/files',NULL,NULL,NULL,NOW(),NOW(),'统一文件上传、管理、预览与存储配置入口',0,NULL,NOW(),NULL,NOW())
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
`del_flag` = VALUES(`del_flag`),
`update_time` = NOW(),
`updated_at` = NOW();

UPDATE `authorization_menu`
SET `parent_id` = 28,
    `menu_type` = 2,
    `menu_name` = '文件管理',
    `menu_code` = 'file:files',
    `path` = '/file/files',
    `icon` = 'Files',
    `component` = '@/views/file/files/index.vue',
    `sort` = 1,
    `status` = 1,
    `visible` = 1,
    `redirect` = NULL,
    `permissions` = 'file:files:list',
    `remark` = '文件记录、上传下载、预览和归档管理',
    `del_flag` = 0,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22;

UPDATE `authorization_menu`
SET `parent_id` = 28,
    `menu_type` = 2,
    `menu_name` = '存储配置',
    `menu_code` = 'file:storage-configs',
    `path` = '/file/storage-configs',
    `icon` = 'Setting',
    `component` = '@/views/file/storage-configs/index.vue',
    `sort` = 2,
    `status` = 1,
    `visible` = 1,
    `redirect` = NULL,
    `permissions` = 'file:storage-configs:list',
    `remark` = '文件底层存储配置管理',
    `del_flag` = 0,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 23;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:list',
    `permissions` = 'file:files:list',
    `icon` = 'Search',
    `remark` = '文件记录列表查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22001;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:query',
    `permissions` = 'file:files:query',
    `icon` = 'View',
    `remark` = '文件详情与预览元数据查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22002;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:upload',
    `permissions` = 'file:files:upload',
    `icon` = 'Upload',
    `remark` = '文件管理上传权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22003;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:download',
    `permissions` = 'file:files:download',
    `icon` = 'Download',
    `remark` = '文件下载权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22004;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:archive',
    `permissions` = 'file:files:archive',
    `icon` = 'Delete',
    `remark` = '文件归档权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22005;

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(22006,1,'internal-admin',22,3,'目录查询','file:directories:list',NULL,'FolderSearch',NULL,6,1,0,0,0,NULL,'file:directories:list',NULL,NULL,NOW(),NOW(),'文件目录树查询权限',0,NULL,NOW(),NULL,NOW()),
(22007,1,'internal-admin',22,3,'目录新增','file:directories:add',NULL,'FolderPlus',NULL,7,1,0,0,0,NULL,'file:directories:add',NULL,NULL,NOW(),NOW(),'文件目录新增权限',0,NULL,NOW(),NULL,NOW()),
(22008,1,'internal-admin',22,3,'目录编辑','file:directories:edit',NULL,'Edit',NULL,8,1,0,0,0,NULL,'file:directories:edit',NULL,NULL,NOW(),NOW(),'文件目录编辑权限',0,NULL,NOW(),NULL,NOW()),
(22009,1,'internal-admin',22,3,'目录删除','file:directories:delete',NULL,'FolderDelete',NULL,9,1,0,0,0,NULL,'file:directories:delete',NULL,NULL,NOW(),NOW(),'文件目录删除权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`menu_type` = VALUES(`menu_type`),
`menu_name` = VALUES(`menu_name`),
`menu_code` = VALUES(`menu_code`),
`icon` = VALUES(`icon`),
`sort` = VALUES(`sort`),
`status` = VALUES(`status`),
`visible` = VALUES(`visible`),
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`),
`update_time` = NOW(),
`updated_at` = NOW();

UPDATE `authorization_menu`
SET `menu_code` = REPLACE(`menu_code`, 'system:file-storage', 'file:storage-configs'),
    `permissions` = REPLACE(`permissions`, 'system:file-storage', 'file:storage-configs'),
    `icon` = CASE `id`
        WHEN 23001 THEN 'Search'
        WHEN 23002 THEN 'View'
        WHEN 23003 THEN 'CirclePlus'
        WHEN 23004 THEN 'Edit'
        WHEN 23005 THEN 'Delete'
        WHEN 23006 THEN 'Connection'
        WHEN 23007 THEN 'CircleCheck'
        ELSE `icon`
    END,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` BETWEEN 23001 AND 23007;

INSERT INTO `file_storage_config` (`id`, `tenant_id`, `config_name`, `storage_type`, `endpoint`, `public_endpoint`, `region`, `bucket_name`, `access_key`, `secret_key`, `path_style_access`, `ssl_enabled`, `active`, `status`, `remark`, `created_by`, `created_time`, `created_at`, `updated_by`, `updated_time`, `updated_at`)
VALUES
(2,1,'MinIO 本地联调','MINIO','http://127.0.0.1:9000','http://file.mango.io:9000','us-east-1','mango-file','minioadmin','minioadmin',1,0,0,1,'本地 Docker Compose MinIO 示例配置。启动 mango-file/docker-compose.yml 后可设为默认验证对象存储。',NULL,NOW(),NOW(),NULL,NOW(),NOW())
ON DUPLICATE KEY UPDATE
`storage_type` = VALUES(`storage_type`),
`endpoint` = VALUES(`endpoint`),
`public_endpoint` = VALUES(`public_endpoint`),
`region` = VALUES(`region`),
`bucket_name` = VALUES(`bucket_name`),
`access_key` = VALUES(`access_key`),
`secret_key` = VALUES(`secret_key`),
`path_style_access` = VALUES(`path_style_access`),
`ssl_enabled` = VALUES(`ssl_enabled`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`updated_time` = NOW(),
`updated_at` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1097,1,1,28,97),
(1098,1,1,22,98),
(1099,1,1,23,99),
(1100,1,1,22001,100),
(1101,1,1,22002,101),
(1102,1,1,22003,102),
(1103,1,1,22004,103),
(1104,1,1,22005,104),
(1105,1,1,22006,105),
(1106,1,1,22007,106),
(1107,1,1,22008,107),
(1108,1,1,22009,108),
(1109,1,1,23001,109),
(1110,1,1,23002,110),
(1111,1,1,23003,111),
(1115,1,1,23004,112),
(1116,1,1,23005,113),
(1117,1,1,23006,114),
(1118,1,1,23007,115);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 500000 + `menu_id`, 1, 1, `menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu_package_item`
WHERE `package_id` = 1
  AND `menu_id` IN (28,22,23,22001,22002,22003,22004,22005,22006,22007,22008,22009,23001,23002,23003,23004,23005,23006,23007);



-- -----------------------------------------------------------------------------
-- Folded from V3__file_settings.sql
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `file_settings` (
  `id` bigint NOT NULL COMMENT '文件中心配置ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '机构隔离ID，用户可见语义为机构，技术字段保留tenant_id',
  `max_size` bigint NOT NULL DEFAULT '104857600' COMMENT '单文件最大大小，单位字节',
  `allowed_extensions` varchar(1000) DEFAULT NULL COMMENT '允许上传扩展名，逗号分隔；为空表示不限制',
  `blocked_extensions` varchar(1000) DEFAULT 'exe,bat,cmd,sh,jar' COMMENT '禁止上传扩展名，逗号分隔',
  `instant_upload_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用秒传: 0-否 1-是',
  `direct_upload_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用客户端直传对象存储: 0-否 1-是',
  `direct_upload_expire_seconds` bigint NOT NULL DEFAULT '900' COMMENT '直传签名有效期，单位秒',
  `access_token_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用限时访问令牌: 0-否 1-是',
  `access_token_expire_seconds` bigint NOT NULL DEFAULT '600' COMMENT '访问令牌有效期，单位秒',
  `preview_provider_url` varchar(500) DEFAULT NULL COMMENT '外部文档预览服务地址',
  `preview_expire_seconds` bigint NOT NULL DEFAULT '600' COMMENT '预览访问有效期，单位秒',
  `preview_external_extensions` varchar(1000) DEFAULT 'doc,docx,xls,xlsx,xlsm,ppt,pptx,odt,ods,odp,ofd,wps,et,dps,csv,txt,zip,rar,7z,eml,msg' COMMENT '外部预览扩展名，逗号分隔',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_settings_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件中心运行时配置表';

INSERT INTO `file_settings` (`id`, `tenant_id`, `max_size`, `allowed_extensions`, `blocked_extensions`, `instant_upload_enabled`, `direct_upload_enabled`, `direct_upload_expire_seconds`, `access_token_enabled`, `access_token_expire_seconds`, `preview_provider_url`, `preview_expire_seconds`, `preview_external_extensions`, `created_by`, `created_time`, `created_at`, `updated_by`, `updated_time`, `updated_at`)
VALUES
(1,1,104857600,NULL,'exe,bat,cmd,sh,jar',1,0,900,0,600,NULL,600,'doc,docx,xls,xlsx,xlsm,ppt,pptx,odt,ods,odp,ofd,wps,et,dps,csv,txt,zip,rar,7z,eml,msg',NULL,NOW(),NOW(),NULL,NOW(),NOW())
ON DUPLICATE KEY UPDATE
`updated_time` = `updated_time`;

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(24,1,'internal-admin',28,2,'文件配置','file:settings','/file/settings','Tools','@/views/file/settings/index.vue',3,1,1,0,0,NULL,'file:settings:query',NULL,NULL,NOW(),NOW(),'文件上传策略、访问策略、预览策略和直传策略配置',0,NULL,NOW(),NULL,NOW()),
(24001,1,'internal-admin',24,3,'查询','file:settings:query',NULL,'Search',NULL,1,1,0,0,0,NULL,'file:settings:query',NULL,NULL,NOW(),NOW(),'文件中心配置查询权限',0,NULL,NOW(),NULL,NOW()),
(24002,1,'internal-admin',24,3,'编辑','file:settings:edit',NULL,'Edit',NULL,2,1,0,0,0,NULL,'file:settings:edit',NULL,NULL,NOW(),NOW(),'文件中心配置编辑权限',0,NULL,NOW(),NULL,NOW())
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
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`),
`update_time` = NOW(),
`updated_at` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1112,1,1,24,112),
(1113,1,1,24001,113),
(1114,1,1,24002,114);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 500000 + `menu_id`, 1, 1, `menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu_package_item`
WHERE `package_id` = 1
  AND `menu_id` IN (24,24001,24002);



-- -----------------------------------------------------------------------------
-- Folded from V4__file_directory_and_storage_path.sql
-- -----------------------------------------------------------------------------

SET @add_storage_path = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_storage_config'
              AND column_name = 'storage_path'
        ),
        'ALTER TABLE `file_storage_config` ADD COLUMN `storage_path` varchar(255) NOT NULL DEFAULT '''' COMMENT ''存储路径前缀'' AFTER `bucket_name`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_storage_path;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_directory_id = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_record'
              AND column_name = 'directory_id'
        ),
        'ALTER TABLE `file_record` ADD COLUMN `directory_id` bigint NOT NULL DEFAULT ''0'' COMMENT ''逻辑目录ID，根目录为0'' AFTER `purpose`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_directory_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_directory_index = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'file_record'
              AND index_name = 'idx_file_directory'
        ),
        'ALTER TABLE `file_record` ADD KEY `idx_file_directory` (`tenant_id`, `directory_id`, `archived`, `created_time`)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_directory_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `file_directory` (
  `id` bigint NOT NULL COMMENT '目录ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID，用户可见语义为机构，技术字段保留tenant_id',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父目录ID，根目录为0',
  `directory_name` varchar(128) NOT NULL COMMENT '目录名称',
  `directory_path` varchar(1000) NOT NULL DEFAULT '/' COMMENT '目录路径，按ID物化路径',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序值',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用 1-启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_directory_name` (`tenant_id`,`parent_id`,`directory_name`),
  KEY `idx_file_directory_parent` (`tenant_id`,`parent_id`,`sort`),
  KEY `idx_file_directory_path` (`tenant_id`,`directory_path`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件逻辑目录表';

UPDATE `file_storage_config`
SET `storage_path` = 'mango-file'
WHERE (`storage_path` IS NULL OR `storage_path` = '')
  AND `storage_type` = 'LOCAL'
  AND `bucket_name` = 'local';



-- -----------------------------------------------------------------------------
-- Folded from V5__file_settings_production_policy.sql
-- -----------------------------------------------------------------------------

SET @add_default_access_level = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'default_access_level'),
        'ALTER TABLE `file_settings` ADD COLUMN `default_access_level` varchar(32) NOT NULL DEFAULT ''PRIVATE'' COMMENT ''默认访问级别'' AFTER `blocked_extensions`',
        'SELECT 1')
);
PREPARE stmt FROM @add_default_access_level;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_duplicate_name_strategy = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'duplicate_name_strategy'),
        'ALTER TABLE `file_settings` ADD COLUMN `duplicate_name_strategy` varchar(32) NOT NULL DEFAULT ''REJECT'' COMMENT ''重名处理策略: REJECT AUTO_RENAME ALLOW'' AFTER `default_access_level`',
        'SELECT 1')
);
PREPARE stmt FROM @add_duplicate_name_strategy;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_duplicate_check_directory_scoped = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'duplicate_check_directory_scoped'),
        'ALTER TABLE `file_settings` ADD COLUMN `duplicate_check_directory_scoped` tinyint NOT NULL DEFAULT ''1'' COMMENT ''是否按逻辑目录隔离重名'' AFTER `duplicate_name_strategy`',
        'SELECT 1')
);
PREPARE stmt FROM @add_duplicate_check_directory_scoped;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_object_name_strategy = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'object_name_strategy'),
        'ALTER TABLE `file_settings` ADD COLUMN `object_name_strategy` varchar(32) NOT NULL DEFAULT ''DATE_UUID'' COMMENT ''对象命名策略: DATE_UUID HASH ORIGINAL'' AFTER `duplicate_check_directory_scoped`',
        'SELECT 1')
);
PREPARE stmt FROM @add_object_name_strategy;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_instant_upload_scope = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'instant_upload_scope'),
        'ALTER TABLE `file_settings` ADD COLUMN `instant_upload_scope` varchar(32) NOT NULL DEFAULT ''TENANT'' COMMENT ''秒传匹配范围: TENANT GLOBAL'' AFTER `instant_upload_enabled`',
        'SELECT 1')
);
PREPARE stmt FROM @add_instant_upload_scope;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_content_type_check_enabled = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'content_type_check_enabled'),
        'ALTER TABLE `file_settings` ADD COLUMN `content_type_check_enabled` tinyint NOT NULL DEFAULT ''1'' COMMENT ''是否校验内容类型'' AFTER `instant_upload_scope`',
        'SELECT 1')
);
PREPARE stmt FROM @add_content_type_check_enabled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_allowed_content_types = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'allowed_content_types'),
        'ALTER TABLE `file_settings` ADD COLUMN `allowed_content_types` varchar(1000) DEFAULT NULL COMMENT ''允许上传内容类型，逗号分隔；为空表示不限制'' AFTER `content_type_check_enabled`',
        'SELECT 1')
);
PREPARE stmt FROM @add_allowed_content_types;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_blocked_content_types = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'blocked_content_types'),
        'ALTER TABLE `file_settings` ADD COLUMN `blocked_content_types` varchar(1000) DEFAULT ''application/x-msdownload,application/x-sh'' COMMENT ''禁止上传内容类型，逗号分隔'' AFTER `allowed_content_types`',
        'SELECT 1')
);
PREPARE stmt FROM @add_blocked_content_types;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_public_read_requires_token = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'public_read_requires_token'),
        'ALTER TABLE `file_settings` ADD COLUMN `public_read_requires_token` tinyint NOT NULL DEFAULT ''0'' COMMENT ''公开读取文件是否仍强制签名访问'' AFTER `access_token_enabled`',
        'SELECT 1')
);
PREPARE stmt FROM @add_public_read_requires_token;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_archive_retain_enabled = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'archive_retain_enabled'),
        'ALTER TABLE `file_settings` ADD COLUMN `archive_retain_enabled` tinyint NOT NULL DEFAULT ''1'' COMMENT ''是否保留归档记录'' AFTER `preview_external_extensions`',
        'SELECT 1')
);
PREPARE stmt FROM @add_archive_retain_enabled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_archive_retain_days = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'archive_retain_days'),
        'ALTER TABLE `file_settings` ADD COLUMN `archive_retain_days` int NOT NULL DEFAULT ''180'' COMMENT ''归档记录保留天数'' AFTER `archive_retain_enabled`',
        'SELECT 1')
);
PREPARE stmt FROM @add_archive_retain_days;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_archive_restore_enabled = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'archive_restore_enabled'),
        'ALTER TABLE `file_settings` ADD COLUMN `archive_restore_enabled` tinyint NOT NULL DEFAULT ''0'' COMMENT ''是否允许恢复归档'' AFTER `archive_retain_days`',
        'SELECT 1')
);
PREPARE stmt FROM @add_archive_restore_enabled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_physical_delete_enabled = (
    SELECT IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_settings' AND column_name = 'physical_delete_enabled'),
        'ALTER TABLE `file_settings` ADD COLUMN `physical_delete_enabled` tinyint NOT NULL DEFAULT ''0'' COMMENT ''是否删除物理对象'' AFTER `archive_restore_enabled`',
        'SELECT 1')
);
PREPARE stmt FROM @add_physical_delete_enabled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `file_settings`
SET `default_access_level` = COALESCE(NULLIF(`default_access_level`, ''), 'PRIVATE'),
    `duplicate_name_strategy` = COALESCE(NULLIF(`duplicate_name_strategy`, ''), 'REJECT'),
    `duplicate_check_directory_scoped` = COALESCE(`duplicate_check_directory_scoped`, 1),
    `object_name_strategy` = COALESCE(NULLIF(`object_name_strategy`, ''), 'DATE_UUID'),
    `instant_upload_scope` = COALESCE(NULLIF(`instant_upload_scope`, ''), 'TENANT'),
    `content_type_check_enabled` = COALESCE(`content_type_check_enabled`, 1),
    `blocked_content_types` = COALESCE(NULLIF(`blocked_content_types`, ''), 'application/x-msdownload,application/x-sh'),
    `public_read_requires_token` = COALESCE(`public_read_requires_token`, 0),
    `archive_retain_enabled` = COALESCE(`archive_retain_enabled`, 1),
    `archive_retain_days` = COALESCE(`archive_retain_days`, 180),
    `archive_restore_enabled` = COALESCE(`archive_restore_enabled`, 0),
    `physical_delete_enabled` = COALESCE(`physical_delete_enabled`, 0);



-- -----------------------------------------------------------------------------
-- Folded from V6__file_object_reference_index.sql
-- -----------------------------------------------------------------------------

SET @drop_file_object_unique_index = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'file_record'
              AND index_name = 'uk_file_object'
        ),
        'ALTER TABLE `file_record` DROP INDEX `uk_file_object`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_file_object_unique_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_object_index = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'file_record'
              AND index_name = 'idx_file_object'
        ),
        'ALTER TABLE `file_record` ADD KEY `idx_file_object` (`bucket_name`,`object_name`)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_object_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;



-- -----------------------------------------------------------------------------
-- Folded from V7__rename_sys_file_tables.sql
-- -----------------------------------------------------------------------------

SET @rename_file_record = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_file_record')
        AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'file_record'),
        'RENAME TABLE `sys_file_record` TO `file_record`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_file_record;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_file_storage_config = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_file_storage_config')
        AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'file_storage_config'),
        'RENAME TABLE `sys_file_storage_config` TO `file_storage_config`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_file_storage_config;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_file_settings = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_file_settings')
        AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'file_settings'),
        'RENAME TABLE `sys_file_settings` TO `file_settings`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_file_settings;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_file_directory = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_file_directory')
        AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'file_directory'),
        'RENAME TABLE `sys_file_directory` TO `file_directory`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_file_directory;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;



-- -----------------------------------------------------------------------------
-- Folded from V8__file_record_biz_meta.sql
-- -----------------------------------------------------------------------------

SET @add_biz_meta = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_record'
              AND column_name = 'biz_meta'
        ),
        'ALTER TABLE `file_record` ADD COLUMN `biz_meta` json DEFAULT NULL COMMENT ''业务自定义参数JSON'' AFTER `purpose`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_biz_meta;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;



-- -----------------------------------------------------------------------------
-- Folded from V9__file_access_mode.sql
-- -----------------------------------------------------------------------------

SET @add_access_mode = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_settings'
              AND column_name = 'access_mode'
        ),
        'ALTER TABLE `file_settings` ADD COLUMN `access_mode` varchar(32) NOT NULL DEFAULT ''PROXY'' COMMENT ''文件访问模式: PROXY-Java服务转发 DIRECT-直连底层存储'' AFTER `public_read_requires_token`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_access_mode;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `file_settings`
SET `access_mode` = COALESCE(NULLIF(`access_mode`, ''), 'PROXY');


-- -----------------------------------------------------------------------------
-- Folded from V2__file_object_and_upload_session.sql
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `file_object` (
  `id` bigint NOT NULL COMMENT '物理文件对象ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `storage_config_id` bigint DEFAULT NULL COMMENT '存储配置ID',
  `storage_type` varchar(32) NOT NULL COMMENT '存储类型',
  `bucket_name` varchar(128) NOT NULL COMMENT '存储桶名称',
  `object_name` varchar(500) NOT NULL COMMENT '对象名称',
  `file_hash` varchar(128) NOT NULL COMMENT '文件SHA-256哈希',
  `file_size` bigint NOT NULL COMMENT '文件大小，单位字节',
  `content_type` varchar(128) DEFAULT NULL COMMENT '内容类型',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-上传中 1-完成 2-失败 3-无引用 9-已删除',
  `ref_count` bigint NOT NULL DEFAULT '0' COMMENT '业务文件记录引用数',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_object_hash_storage` (`storage_config_id`,`bucket_name`,`file_hash`,`file_size`),
  KEY `idx_file_object_location` (`storage_config_id`,`bucket_name`,`object_name`),
  KEY `idx_file_object_hash` (`file_hash`,`file_size`),
  KEY `idx_file_object_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物理文件对象表';

CREATE TABLE IF NOT EXISTS `file_hash_mapping` (
  `id` bigint NOT NULL COMMENT '秒传映射ID',
  `scope_type` varchar(32) NOT NULL COMMENT '秒传范围: TENANT-机构 GLOBAL-全局',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '机构隔离ID；GLOBAL时为0',
  `storage_config_id` bigint DEFAULT NULL COMMENT '存储配置ID',
  `file_hash` varchar(128) NOT NULL COMMENT '文件SHA-256哈希',
  `file_size` bigint NOT NULL COMMENT '文件大小，单位字节',
  `object_id` bigint NOT NULL COMMENT '物理文件对象ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用 1-启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_hash_mapping_target` (`scope_type`,`tenant_id`,`storage_config_id`,`file_hash`,`file_size`),
  KEY `idx_file_hash_mapping_object` (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件秒传哈希映射表';

CREATE TABLE IF NOT EXISTS `file_upload_session` (
  `id` bigint NOT NULL COMMENT '上传会话ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `storage_config_id` bigint DEFAULT NULL COMMENT '存储配置ID',
  `storage_type` varchar(32) NOT NULL COMMENT '存储类型',
  `bucket_name` varchar(128) NOT NULL COMMENT '存储桶名称',
  `object_name` varchar(500) NOT NULL COMMENT '对象名称',
  `upload_mode` varchar(32) NOT NULL COMMENT '上传模式: SERVER_CHUNK S3_MULTIPART',
  `storage_upload_id` varchar(255) DEFAULT NULL COMMENT '对象存储原生分片上传ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_ext` varchar(32) DEFAULT NULL COMMENT '文件扩展名',
  `file_hash` varchar(128) NOT NULL COMMENT '文件SHA-256哈希',
  `file_size` bigint NOT NULL COMMENT '文件大小，单位字节',
  `content_type` varchar(128) DEFAULT NULL COMMENT '内容类型',
  `chunk_size` bigint NOT NULL COMMENT '分片大小，单位字节',
  `total_parts` int NOT NULL COMMENT '总分片数',
  `uploaded_parts` int NOT NULL DEFAULT '0' COMMENT '已上传分片数',
  `status` varchar(32) NOT NULL COMMENT '状态: INIT UPLOADING COMPLETING COMPLETED FAILED ABORTED EXPIRED',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `purpose` varchar(64) DEFAULT NULL COMMENT '文件用途',
  `access_level` varchar(32) NOT NULL DEFAULT 'PRIVATE' COMMENT '访问级别',
  `biz_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `biz_id` varchar(128) DEFAULT NULL COMMENT '业务ID',
  `biz_meta` json DEFAULT NULL COMMENT '业务自定义参数JSON',
  `directory_id` bigint NOT NULL DEFAULT '0' COMMENT '逻辑目录ID',
  `object_id` bigint DEFAULT NULL COMMENT '物理文件对象ID',
  `file_record_id` bigint DEFAULT NULL COMMENT '文件记录ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_upload_session_tenant` (`tenant_id`,`status`),
  KEY `idx_file_upload_session_expires` (`expires_at`),
  KEY `idx_file_upload_session_hash` (`storage_config_id`,`file_hash`,`file_size`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分片上传会话表';

CREATE TABLE IF NOT EXISTS `file_upload_part` (
  `id` bigint NOT NULL COMMENT '上传分片ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `session_id` bigint NOT NULL COMMENT '上传会话ID',
  `part_number` int NOT NULL COMMENT '分片序号，从1开始',
  `part_size` bigint DEFAULT NULL COMMENT '分片大小，单位字节',
  `part_hash` varchar(128) DEFAULT NULL COMMENT '分片哈希',
  `etag` varchar(255) DEFAULT NULL COMMENT '对象存储返回的ETag',
  `status` varchar(32) NOT NULL COMMENT '状态: COMPLETED',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_upload_part` (`session_id`,`part_number`),
  KEY `idx_file_upload_part_tenant` (`tenant_id`,`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分片上传明细表';

UPDATE `file_storage_config`
SET `active` = CASE WHEN `id` = 2 THEN 1 ELSE 0 END,
    `updated_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (1, 2);

UPDATE `file_settings`
SET `access_mode` = 'DIRECT',
    `access_token_enabled` = 1,
    `public_read_requires_token` = 1,
    `direct_upload_enabled` = 1,
    `updated_time` = NOW(),
    `updated_at` = NOW()
WHERE `tenant_id` = 1;

-- -----------------------------------------------------------------------------
-- Squashed from: V2__file_object_and_upload_session.sql
-- -----------------------------------------------------------------------------
-- 文件对象、秒传映射与分片上传会话。

CREATE TABLE IF NOT EXISTS `file_object` (
  `id` bigint NOT NULL COMMENT '物理文件对象ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `storage_config_id` bigint DEFAULT NULL COMMENT '存储配置ID',
  `storage_type` varchar(32) NOT NULL COMMENT '存储类型',
  `bucket_name` varchar(128) NOT NULL COMMENT '存储桶名称',
  `object_name` varchar(500) NOT NULL COMMENT '对象名称',
  `file_hash` varchar(128) NOT NULL COMMENT '文件SHA-256哈希',
  `file_size` bigint NOT NULL COMMENT '文件大小，单位字节',
  `content_type` varchar(128) DEFAULT NULL COMMENT '内容类型',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-上传中 1-完成 2-失败 3-无引用 9-已删除',
  `ref_count` bigint NOT NULL DEFAULT '0' COMMENT '业务文件记录引用数',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_object_hash_storage` (`storage_config_id`,`bucket_name`,`file_hash`,`file_size`),
  KEY `idx_file_object_location` (`storage_config_id`,`bucket_name`,`object_name`),
  KEY `idx_file_object_hash` (`file_hash`,`file_size`),
  KEY `idx_file_object_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物理文件对象表';

CREATE TABLE IF NOT EXISTS `file_hash_mapping` (
  `id` bigint NOT NULL COMMENT '秒传映射ID',
  `scope_type` varchar(32) NOT NULL COMMENT '秒传范围: TENANT-机构 GLOBAL-全局',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '机构隔离ID；GLOBAL时为0',
  `storage_config_id` bigint DEFAULT NULL COMMENT '存储配置ID',
  `file_hash` varchar(128) NOT NULL COMMENT '文件SHA-256哈希',
  `file_size` bigint NOT NULL COMMENT '文件大小，单位字节',
  `object_id` bigint NOT NULL COMMENT '物理文件对象ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用 1-启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_hash_mapping_target` (`scope_type`,`tenant_id`,`storage_config_id`,`file_hash`,`file_size`),
  KEY `idx_file_hash_mapping_object` (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件秒传哈希映射表';

CREATE TABLE IF NOT EXISTS `file_upload_session` (
  `id` bigint NOT NULL COMMENT '上传会话ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `storage_config_id` bigint DEFAULT NULL COMMENT '存储配置ID',
  `storage_type` varchar(32) NOT NULL COMMENT '存储类型',
  `bucket_name` varchar(128) NOT NULL COMMENT '存储桶名称',
  `object_name` varchar(500) NOT NULL COMMENT '对象名称',
  `upload_mode` varchar(32) NOT NULL COMMENT '上传模式: SERVER_CHUNK S3_MULTIPART',
  `storage_upload_id` varchar(255) DEFAULT NULL COMMENT '对象存储原生分片上传ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_ext` varchar(32) DEFAULT NULL COMMENT '文件扩展名',
  `file_hash` varchar(128) NOT NULL COMMENT '文件SHA-256哈希',
  `file_size` bigint NOT NULL COMMENT '文件大小，单位字节',
  `content_type` varchar(128) DEFAULT NULL COMMENT '内容类型',
  `chunk_size` bigint NOT NULL COMMENT '分片大小，单位字节',
  `total_parts` int NOT NULL COMMENT '总分片数',
  `uploaded_parts` int NOT NULL DEFAULT '0' COMMENT '已上传分片数',
  `status` varchar(32) NOT NULL COMMENT '状态: INIT UPLOADING COMPLETING COMPLETED FAILED ABORTED EXPIRED',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `purpose` varchar(64) DEFAULT NULL COMMENT '文件用途',
  `access_level` varchar(32) NOT NULL DEFAULT 'PRIVATE' COMMENT '访问级别',
  `biz_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `biz_id` varchar(128) DEFAULT NULL COMMENT '业务ID',
  `biz_meta` json DEFAULT NULL COMMENT '业务自定义参数JSON',
  `directory_id` bigint NOT NULL DEFAULT '0' COMMENT '逻辑目录ID',
  `object_id` bigint DEFAULT NULL COMMENT '物理文件对象ID',
  `file_record_id` bigint DEFAULT NULL COMMENT '文件记录ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_upload_session_tenant` (`tenant_id`,`status`),
  KEY `idx_file_upload_session_expires` (`expires_at`),
  KEY `idx_file_upload_session_hash` (`storage_config_id`,`file_hash`,`file_size`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分片上传会话表';

CREATE TABLE IF NOT EXISTS `file_upload_part` (
  `id` bigint NOT NULL COMMENT '上传分片ID',
  `tenant_id` bigint NOT NULL COMMENT '机构隔离ID',
  `session_id` bigint NOT NULL COMMENT '上传会话ID',
  `part_number` int NOT NULL COMMENT '分片序号，从1开始',
  `part_size` bigint DEFAULT NULL COMMENT '分片大小，单位字节',
  `part_hash` varchar(128) DEFAULT NULL COMMENT '分片哈希',
  `etag` varchar(255) DEFAULT NULL COMMENT '对象存储返回的ETag',
  `status` varchar(32) NOT NULL COMMENT '状态: COMPLETED',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_upload_part` (`session_id`,`part_number`),
  KEY `idx_file_upload_part_tenant` (`tenant_id`,`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分片上传明细表';

SET @add_file_object_created_by = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_object'
              AND column_name = 'created_by'
        ),
        'ALTER TABLE `file_object` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人ID'' AFTER `ref_count`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_object_created_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_object_updated_by = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_object'
              AND column_name = 'updated_by'
        ),
        'ALTER TABLE `file_object` ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT ''更新人ID'' AFTER `created_at`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_object_updated_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_hash_mapping_created_by = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_hash_mapping'
              AND column_name = 'created_by'
        ),
        'ALTER TABLE `file_hash_mapping` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人ID'' AFTER `status`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_hash_mapping_created_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_hash_mapping_updated_by = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_hash_mapping'
              AND column_name = 'updated_by'
        ),
        'ALTER TABLE `file_hash_mapping` ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT ''更新人ID'' AFTER `created_at`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_hash_mapping_updated_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_upload_part_tenant_id = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_upload_part'
              AND column_name = 'tenant_id'
        ),
        'ALTER TABLE `file_upload_part` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT ''机构隔离ID'' AFTER `id`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_upload_part_tenant_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_upload_part_created_by = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_upload_part'
              AND column_name = 'created_by'
        ),
        'ALTER TABLE `file_upload_part` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人ID'' AFTER `status`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_upload_part_created_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_upload_part_updated_by = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_upload_part'
              AND column_name = 'updated_by'
        ),
        'ALTER TABLE `file_upload_part` ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT ''更新人ID'' AFTER `created_at`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_upload_part_updated_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_upload_part_tenant_index = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'file_upload_part'
              AND index_name = 'idx_file_upload_part_tenant'
        ),
        'ALTER TABLE `file_upload_part` ADD KEY `idx_file_upload_part_tenant` (`tenant_id`,`session_id`)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_upload_part_tenant_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_record_object_id = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'file_record'
              AND column_name = 'object_id'
        ),
        'ALTER TABLE `file_record` ADD COLUMN `object_id` bigint DEFAULT NULL COMMENT ''物理文件对象ID'' AFTER `access_level`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_record_object_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_file_record_object_id_index = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'file_record'
              AND index_name = 'idx_file_record_object_id'
        ),
        'ALTER TABLE `file_record` ADD KEY `idx_file_record_object_id` (`object_id`)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_file_record_object_id_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DELETE FROM `file_upload_part`;
DELETE FROM `file_upload_session`;
DELETE FROM `file_hash_mapping`;
DELETE FROM `file_object`;
DELETE FROM `file_record`;

UPDATE `file_storage_config`
SET `active` = CASE WHEN `id` = 2 THEN 1 ELSE 0 END,
    `updated_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (1, 2);

UPDATE `file_settings`
SET `access_mode` = 'DIRECT',
    `access_token_enabled` = 1,
    `public_read_requires_token` = 1,
    `direct_upload_enabled` = 1,
    `updated_time` = NOW(),
    `updated_at` = NOW()
WHERE `tenant_id` = 1;

-- -----------------------------------------------------------------------------
-- Squashed from: V3__default_file_preview_provider_url.sql
-- -----------------------------------------------------------------------------
ALTER TABLE `file_settings`
    MODIFY COLUMN `preview_provider_url` varchar(500) DEFAULT '/file-preview/files/preview' COMMENT '外部文档预览服务地址';

UPDATE `file_settings`
SET `preview_provider_url` = '/file-preview/files/preview'
WHERE `preview_provider_url` IS NULL
   OR TRIM(`preview_provider_url`) = '';

-- -----------------------------------------------------------------------------
-- Squashed from: V4__default_file_preview_external_extensions.sql
-- -----------------------------------------------------------------------------
UPDATE `file_settings`
SET `preview_external_extensions` = 'txt,csv,json,xml,png,jpg,jpeg,gif,webp,svg,pdf,ofd,doc,docx,xls,xlsx,ppt,pptx,odt,ods,odp,zip,rar,7z'
WHERE `preview_external_extensions` IS NULL
   OR TRIM(`preview_external_extensions`) = ''
   OR `preview_external_extensions` = 'doc,docx,xls,xlsx,ppt,pptx,ofd';

-- -----------------------------------------------------------------------------
-- Squashed from: V5__default_local_storage_active.sql
-- -----------------------------------------------------------------------------
UPDATE `file_storage_config`
SET `active` = CASE WHEN `storage_type` = 'LOCAL' THEN 1 ELSE 0 END,
    `updated_time` = NOW(),
    `updated_at` = NOW()
WHERE `storage_type` IN ('LOCAL', 'MINIO');

-- -----------------------------------------------------------------------------
-- Squashed from: V6__file_delete_permission.sql
-- -----------------------------------------------------------------------------
INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(22010,1,'internal-admin',22,3,'删除文件','file:files:delete',NULL,'Delete',NULL,10,1,0,0,0,NULL,'file:files:delete',NULL,NULL,NOW(),NOW(),'文件记录删除权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`menu_type` = VALUES(`menu_type`),
`menu_name` = VALUES(`menu_name`),
`menu_code` = VALUES(`menu_code`),
`icon` = VALUES(`icon`),
`sort` = VALUES(`sort`),
`status` = VALUES(`status`),
`visible` = VALUES(`visible`),
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`),
`update_time` = NOW(),
`updated_at` = NOW();
