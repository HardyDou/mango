-- Baseline migration for module: domain

CREATE TABLE IF NOT EXISTS `biz_domain` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` varchar(64) NOT NULL DEFAULT '1' COMMENT '租户标识',
  `domain_code` varchar(64) NOT NULL COMMENT '业务域编码（租户内唯一）',
  `domain_short_code` varchar(64) NOT NULL COMMENT '业务域编码简写（租户内唯一）',
  `domain_name` varchar(128) NOT NULL COMMENT '业务域名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父业务域ID，0=顶级',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(512) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_domain_tenant_code` (`tenant_id`, `domain_code`),
  UNIQUE KEY `uk_biz_domain_tenant_short_code` (`tenant_id`, `domain_short_code`),
  KEY `idx_biz_domain_parent` (`tenant_id`, `parent_id`),
  KEY `idx_biz_domain_status` (`tenant_id`, `status`),
  KEY `idx_biz_domain_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务域管理表';

INSERT INTO `biz_domain`
  (`id`, `tenant_id`, `domain_code`, `domain_short_code`, `domain_name`, `parent_id`, `sort`, `status`, `remark`, `create_time`, `update_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`)
VALUES
  (100, '1', 'COMMON', 'COM', '通用域', 0, 1, 1, '跨模块通用业务域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 0),
  (110, '1', 'WORKFLOW', 'WF', '工作流域', 0, 2, 1, '工作流与审批业务域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 0),
  (120, '1', 'NOTICE', 'NTC', '通知域', 0, 3, 1, '通知中心业务域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 0),
  (130, '1', 'CALENDAR', 'CAL', '日历域', 0, 4, 1, '日历能力业务域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 0),
  (140, '1', 'NUMGEN', 'NUM', '编号域', 0, 5, 1, '编号规则业务域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 0),
  (150, '1', 'FILE', 'FIL', '文件域', 0, 6, 1, '文件能力业务域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 0),
  (160, '1', 'TEMPLATE', 'TPL', '模板域', 0, 7, 1, '模板能力业务域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE
  `domain_short_code` = VALUES(`domain_short_code`),
  `domain_name` = VALUES(`domain_name`),
  `parent_id` = VALUES(`parent_id`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`),
  `deleted` = 0,
  `update_time` = CURRENT_TIMESTAMP,
  `updated_at` = CURRENT_TIMESTAMP;
