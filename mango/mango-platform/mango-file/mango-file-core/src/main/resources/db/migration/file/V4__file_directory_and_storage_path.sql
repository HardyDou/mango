ALTER TABLE `file_storage_config`
    ADD COLUMN `storage_path` varchar(255) NOT NULL DEFAULT '' COMMENT '存储路径前缀' AFTER `bucket_name`;

ALTER TABLE `file_record`
    ADD COLUMN `directory_id` bigint NOT NULL DEFAULT '0' COMMENT '逻辑目录ID，根目录为0' AFTER `purpose`,
    ADD KEY `idx_file_directory` (`tenant_id`, `directory_id`, `archived`, `created_time`);

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
