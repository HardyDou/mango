-- ============================================
-- Mango Organization Post
-- ============================================

CREATE TABLE IF NOT EXISTS `org_post` (
  `id` bigint NOT NULL COMMENT '岗位ID',
  `post_name` varchar(100) NOT NULL COMMENT '岗位名称',
  `post_code` varchar(50) NOT NULL COMMENT '岗位编码',
  `post_sort` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `post_status` char(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_post_code` (`post_code`),
  KEY `idx_org_post_status` (`post_status`),
  KEY `idx_org_post_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

INSERT INTO `org_post` (`id`, `post_name`, `post_code`, `post_sort`, `post_status`, `remark`, `tenant_id`) VALUES
(1, '集团管理员', 'GROUP_ADMIN', 1, '1', '系统默认岗位', 1),
(2, '部门负责人', 'DEPT_MANAGER', 2, '1', '部门管理岗位', 1),
(3, '研发工程师', 'RD_ENGINEER', 3, '1', '研发岗位', 1)
ON DUPLICATE KEY UPDATE
  `post_name` = VALUES(`post_name`),
  `post_sort` = VALUES(`post_sort`),
  `post_status` = VALUES(`post_status`),
  `remark` = VALUES(`remark`);
