-- Baseline migration for module: domain

CREATE TABLE IF NOT EXISTS `biz_domain` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` varchar(64) NOT NULL DEFAULT '1' COMMENT '租户标识',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
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
