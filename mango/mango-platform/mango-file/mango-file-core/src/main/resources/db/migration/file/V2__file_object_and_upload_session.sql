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
