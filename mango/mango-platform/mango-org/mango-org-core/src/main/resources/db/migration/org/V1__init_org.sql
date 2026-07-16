-- Organization schema baseline. Flyway owns DDL only; required and demo data
-- are registered from the org starter resource manifests.

CREATE TABLE IF NOT EXISTS `sys_org` (
  `id` bigint NOT NULL COMMENT '组织ID',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户标识',
  `org_id` bigint DEFAULT NULL COMMENT '数据归属组织标识',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父级组织ID，根节点为0',
  `org_name` varchar(100) NOT NULL COMMENT '组织名称',
  `org_code` varchar(50) NOT NULL COMMENT '组织编码',
  `org_type` int NOT NULL COMMENT '组织类型：1-集团，2-公司，3-部门，4-小组',
  `org_sort` int NOT NULL DEFAULT '0' COMMENT '排序值',
  `org_status` char(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_org_tenant_code` (`tenant_id`, `org_code`),
  KEY `idx_sys_org_tenant_pid` (`tenant_id`, `pid`),
  KEY `idx_sys_org_tenant_type` (`tenant_id`, `org_type`),
  KEY `idx_sys_org_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织表';

CREATE TABLE IF NOT EXISTS `org_post` (
  `id` bigint NOT NULL COMMENT '岗位ID',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户标识',
  `org_id` bigint DEFAULT NULL COMMENT '数据归属组织标识',
  `post_name` varchar(100) NOT NULL COMMENT '岗位名称',
  `post_code` varchar(50) NOT NULL COMMENT '岗位编码',
  `post_sort` int NOT NULL DEFAULT '0' COMMENT '排序值',
  `post_status` char(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_post_tenant_code` (`tenant_id`, `post_code`),
  KEY `idx_org_post_tenant_status` (`tenant_id`, `post_status`),
  KEY `idx_org_post_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';
