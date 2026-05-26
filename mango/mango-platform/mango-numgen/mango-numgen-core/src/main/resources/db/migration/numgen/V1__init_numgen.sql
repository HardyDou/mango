-- Baseline migration for module: numgen
-- Squashed from 5 migration files before first shared release.

-- -----------------------------------------------------------------------------
-- Squashed from: V1__init_numgen.sql
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `numgen_generator` (
  `id` bigint NOT NULL COMMENT '主键',
  `gen_key` varchar(128) NOT NULL COMMENT '业务Key',
  `gen_name` varchar(128) NOT NULL COMMENT '名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `current_rule_version` int DEFAULT NULL COMMENT '当前规则版本',
  `current_publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '当前发布状态',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_numgen_generator_tenant_key` (`tenant_id`, `gen_key`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号生成器';

CREATE TABLE IF NOT EXISTS `numgen_rule` (
  `id` bigint NOT NULL COMMENT '主键',
  `gen_key` varchar(128) NOT NULL COMMENT '业务Key',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `version` int NOT NULL DEFAULT '1' COMMENT '规则版本',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0-未生效，1-生效中',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_numgen_rule_tenant_key_version` (`tenant_id`, `gen_key`, `version`, `del_flag`),
  KEY `idx_numgen_rule_tenant_key_status` (`tenant_id`, `gen_key`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号规则';

CREATE TABLE IF NOT EXISTS `numgen_rule_segment` (
  `id` bigint NOT NULL COMMENT '主键',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `sort_order` int NOT NULL COMMENT '排序',
  `segment_type` varchar(32) NOT NULL COMMENT '片段类型',
  `segment_name` varchar(128) NOT NULL COMMENT '片段名称',
  `literal_value` varchar(128) DEFAULT NULL COMMENT '字符串内容',
  `variable_key` varchar(128) DEFAULT NULL COMMENT '变量键',
  `date_format` varchar(64) DEFAULT NULL COMMENT '日期格式',
  `seq_width` int DEFAULT NULL COMMENT '流水位数',
  `pad_char` varchar(1) DEFAULT '0' COMMENT '补齐字符',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_numgen_rule_segment_rule_order` (`rule_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号规则片段';

CREATE TABLE IF NOT EXISTS `numgen_sequence` (
  `id` bigint NOT NULL COMMENT '主键',
  `gen_key` varchar(128) NOT NULL COMMENT '业务Key',
  `rule_version` int NOT NULL DEFAULT '1' COMMENT '规则版本',
  `current_value` bigint NOT NULL DEFAULT '0' COMMENT '当前序列值',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_numgen_sequence_tenant_rule` (`tenant_id`, `gen_key`, `rule_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号生成序列';

CREATE TABLE IF NOT EXISTS `numgen_history` (
  `id` bigint NOT NULL COMMENT '主键',
  `gen_key` varchar(128) NOT NULL COMMENT '业务Key',
  `rule_id` bigint DEFAULT NULL COMMENT '规则ID',
  `result_no` varchar(256) NOT NULL COMMENT '编号结果',
  `rule_version` int NOT NULL COMMENT '规则版本',
  `biz_key` varchar(128) DEFAULT NULL COMMENT '业务键',
  `input_digest` varchar(256) DEFAULT NULL COMMENT '输入摘要',
  `cost_millis` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-失败，1-成功',
  `error_message` varchar(512) DEFAULT NULL COMMENT '错误信息',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_numgen_history_tenant_key_time` (`tenant_id`, `gen_key`, `create_time`),
  KEY `idx_numgen_history_result` (`result_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编号生成历史';

-- -----------------------------------------------------------------------------
-- Squashed from: V2__tenant_unique_keys.sql
-- -----------------------------------------------------------------------------
-- Tenant-aware keys are created directly by V1 for fresh numgen installs.
-- This migration is intentionally kept as a compatibility placeholder so
-- Flyway version history stays stable without reapplying obsolete key changes.

-- -----------------------------------------------------------------------------
-- Squashed from: V3__simplify_numgen_model.sql
-- -----------------------------------------------------------------------------
SET @schema_name = DATABASE();

SET @drop_generator_description = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_generator` DROP COLUMN `description`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_generator' AND COLUMN_NAME = 'description'
);
PREPARE stmt FROM @drop_generator_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_generator_audit = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_generator` DROP COLUMN `current_audit_status`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_generator' AND COLUMN_NAME = 'current_audit_status'
);
PREPARE stmt FROM @drop_generator_audit;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_audit = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `audit_status`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'audit_status'
);
PREPARE stmt FROM @drop_rule_audit;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_scope_mode = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `scope_mode`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'scope_mode'
);
PREPARE stmt FROM @drop_rule_scope_mode;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_scope_param = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `scope_param`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'scope_param'
);
PREPARE stmt FROM @drop_rule_scope_param;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_reset_period = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `reset_period`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'reset_period'
);
PREPARE stmt FROM @drop_rule_reset_period;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_start_value = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `start_value`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'start_value'
);
PREPARE stmt FROM @drop_rule_start_value;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_step = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `step`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'step'
);
PREPARE stmt FROM @drop_rule_step;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_rule_description = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule` DROP COLUMN `description`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'description'
);
PREPARE stmt FROM @drop_rule_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_segment_required = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule_segment` DROP COLUMN `required`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule_segment' AND COLUMN_NAME = 'required'
);
PREPARE stmt FROM @drop_segment_required;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_segment_default = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule_segment` DROP COLUMN `default_value`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule_segment' AND COLUMN_NAME = 'default_value'
);
PREPARE stmt FROM @drop_segment_default;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_segment_description = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_rule_segment` DROP COLUMN `description`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule_segment' AND COLUMN_NAME = 'description'
);
PREPARE stmt FROM @drop_segment_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_sequence_old_key = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_sequence` DROP INDEX `uk_numgen_sequence_tenant_scope`', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND INDEX_NAME = 'uk_numgen_sequence_tenant_scope'
);
PREPARE stmt FROM @drop_sequence_old_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_sequence_scope = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_sequence` DROP COLUMN `scope_key`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND COLUMN_NAME = 'scope_key'
);
PREPARE stmt FROM @drop_sequence_scope;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_sequence_period = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_sequence` DROP COLUMN `period_key`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND COLUMN_NAME = 'period_key'
);
PREPARE stmt FROM @drop_sequence_period;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_sequence_new_key = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_sequence` ADD UNIQUE KEY `uk_numgen_sequence_tenant_rule` (`tenant_id`, `gen_key`, `rule_version`)', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND INDEX_NAME = 'uk_numgen_sequence_tenant_rule'
);
PREPARE stmt FROM @add_sequence_new_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_history_scope = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_history` DROP COLUMN `scope_key`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_history' AND COLUMN_NAME = 'scope_key'
);
PREPARE stmt FROM @drop_history_scope;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_history_period = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_history` DROP COLUMN `period_key`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_history' AND COLUMN_NAME = 'period_key'
);
PREPARE stmt FROM @drop_history_period;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- Squashed from: V4__add_numgen_rule_version_state.sql
-- -----------------------------------------------------------------------------
SET @schema_name = DATABASE();

SET @add_rule_version_state = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_rule` ADD COLUMN `version_state` varchar(16) NOT NULL DEFAULT ''DRAFT'' COMMENT ''版本状态：DRAFT-草稿，ACTIVE-生效中，HISTORY-历史'' AFTER `publish_status`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND COLUMN_NAME = 'version_state'
);
PREPARE stmt FROM @add_rule_version_state;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `numgen_rule` r
LEFT JOIN `numgen_generator` g
  ON g.`tenant_id` = r.`tenant_id`
 AND g.`gen_key` = r.`gen_key`
 AND g.`del_flag` = 0
SET r.`version_state` = CASE
  WHEN r.`publish_status` = 1 THEN 'ACTIVE'
  WHEN g.`current_rule_version` IS NOT NULL AND r.`version` > g.`current_rule_version` THEN 'DRAFT'
  ELSE 'HISTORY'
END
WHERE r.`del_flag` = 0;

SET @add_rule_version_state_idx = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_rule` ADD KEY `idx_numgen_rule_tenant_key_state` (`tenant_id`, `gen_key`, `version_state`)', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule' AND INDEX_NAME = 'idx_numgen_rule_tenant_key_state'
);
PREPARE stmt FROM @add_rule_version_state_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- Squashed from: V5__segment_sequence_scope.sql
-- -----------------------------------------------------------------------------
SET @schema_name = DATABASE();

SET @add_segment_sequence_scope = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_rule_segment` ADD COLUMN `sequence_scope` tinyint NOT NULL DEFAULT ''0'' COMMENT ''是否参与流水分组：0-否，1-是'' AFTER `pad_char`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_rule_segment' AND COLUMN_NAME = 'sequence_scope'
);
PREPARE stmt FROM @add_segment_sequence_scope;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_sequence_scope_key = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_sequence` ADD COLUMN `scope_key` varchar(256) NOT NULL DEFAULT ''GLOBAL'' COMMENT ''流水分组键'' AFTER `rule_version`', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND COLUMN_NAME = 'scope_key'
);
PREPARE stmt FROM @add_sequence_scope_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `numgen_sequence`
SET `scope_key` = 'GLOBAL'
WHERE `scope_key` IS NULL OR `scope_key` = '';

DELETE s1
FROM `numgen_sequence` s1
JOIN `numgen_sequence` s2
  ON s1.`tenant_id` = s2.`tenant_id`
 AND s1.`gen_key` = s2.`gen_key`
 AND s1.`scope_key` = s2.`scope_key`
 AND (
      s1.`current_value` < s2.`current_value`
      OR (s1.`current_value` = s2.`current_value` AND s1.`id` < s2.`id`)
 )
WHERE s1.`scope_key` = 'GLOBAL';

SET @drop_sequence_rule_key = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `numgen_sequence` DROP INDEX `uk_numgen_sequence_tenant_rule`', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND INDEX_NAME = 'uk_numgen_sequence_tenant_rule'
);
PREPARE stmt FROM @drop_sequence_rule_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_sequence_scope_unique_key = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `numgen_sequence` ADD UNIQUE KEY `uk_numgen_sequence_tenant_scope` (`tenant_id`, `gen_key`, `scope_key`)', 'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'numgen_sequence' AND INDEX_NAME = 'uk_numgen_sequence_tenant_scope'
);
PREPARE stmt FROM @add_sequence_scope_unique_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
