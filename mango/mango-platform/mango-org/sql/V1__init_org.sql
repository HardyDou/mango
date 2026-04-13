-- ============================================
-- Mango Organization Module Init Data
-- ============================================

-- Create sys_org table
CREATE TABLE IF NOT EXISTS `sys_org` (
  `id` bigint NOT NULL COMMENT 'Primary key',
  `pid` bigint NOT NULL DEFAULT 0 COMMENT 'Parent org ID (0 for root)',
  `org_name` varchar(100) NOT NULL COMMENT 'Organization name',
  `org_code` varchar(50) NOT NULL COMMENT 'Organization code (unique)',
  `org_type` int NOT NULL COMMENT 'Org type: 1-集团, 2-公司, 3-部门, 4-小组',
  `org_sort` int NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `org_status` char(1) NOT NULL DEFAULT '1' COMMENT 'Status: 0-disabled, 1-enabled',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_code` (`org_code`),
  KEY `idx_pid` (`pid`),
  KEY `idx_org_type` (`org_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Organization table';

-- ============================================
-- Organization Data (芒果 Organization Tree)
-- ============================================

-- Root: 芒果集团 (Mango Group HQ)
INSERT INTO `sys_org` (`id`, `pid`, `org_name`, `org_code`, `org_type`, `org_sort`, `org_status`, `tenant_id`) VALUES
(1, 0, '芒果集团', 'MANGO_GROUP', 1, 0, '1', 1);

-- Level 2: 子公司 (Subsidiaries)
INSERT INTO `sys_org` (`id`, `pid`, `org_name`, `org_code`, `org_type`, `org_sort`, `org_status`, `tenant_id`) VALUES
(2, 1, '芒果科技', 'MANGO_TECH', 2, 1, '1', 1),
(3, 1, '芒果金融', 'MANGO_FINANCE', 2, 2, '1', 1),
(4, 1, '芒果地产', 'MANGO_REAL_ESTATE', 2, 3, '1', 1);

-- Level 3: 部门 (Departments under 芒果科技)
INSERT INTO `sys_org` (`id`, `pid`, `org_name`, `org_code`, `org_type`, `org_sort`, `org_status`, `tenant_id`) VALUES
(5, 2, '技术研发部', 'TECH_RD', 3, 1, '1', 1),
(6, 2, '产品设计部', 'TECH_PRODUCT', 3, 2, '1', 1),
(7, 2, '市场营销部', 'TECH_MARKETING', 3, 3, '1', 1),
(8, 2, '人力资源部', 'TECH_HR', 3, 4, '1', 1),
(9, 2, '财务部', 'TECH_FINANCE', 3, 5, '1', 1);

-- Level 3: 部门 (Departments under 芒果金融)
INSERT INTO `sys_org` (`id`, `pid`, `org_name`, `org_code`, `org_type`, `org_sort`, `org_status`, `tenant_id`) VALUES
(10, 3, '风险管理部', 'FIN_RISK', 3, 1, '1', 1),
(11, 3, '信贷管理部', 'FIN_CREDIT', 3, 2, '1', 1),
(12, 3, '财务部', 'FIN_ACCOUNTING', 3, 3, '1', 1);

-- Level 3: 部门 (Departments under 芒果地产)
INSERT INTO `sys_org` (`id`, `pid`, `org_name`, `org_code`, `org_type`, `org_sort`, `org_status`, `tenant_id`) VALUES
(13, 4, '项目管理部', 'RE_PROJECT', 3, 1, '1', 1),
(14, 4, '成本合约部', 'RE_CONTRACT', 3, 2, '1', 1),
(15, 4, '财务部', 'RE_FINANCE', 3, 3, '1', 1);

-- Level 4: 小组 (Teams under 技术研发部)
INSERT INTO `sys_org` (`id`, `pid`, `org_name`, `org_code`, `org_type`, `org_sort`, `org_status`, `tenant_id`) VALUES
(16, 5, '后端开发组', 'TECH_BACKEND', 4, 1, '1', 1),
(17, 5, '前端开发组', 'TECH_FRONTEND', 4, 2, '1', 1),
(18, 5, '测试组', 'TECH_QA', 4, 3, '1', 1),
(19, 5, '运维组', 'TECH_OPS', 4, 4, '1', 1);

-- Level 4: 小组 (Teams under 产品设计部)
INSERT INTO `sys_org` (`id`, `pid`, `org_name`, `org_code`, `org_type`, `org_sort`, `org_status`, `tenant_id`) VALUES
(20, 6, '产品策划组', 'PRD_PLANNING', 4, 1, '1', 1),
(21, 6, 'UI设计组', 'PRD_UI', 4, 2, '1', 1),
(22, 6, '交互设计组', 'PRD_UX', 4, 3, '1', 1);
