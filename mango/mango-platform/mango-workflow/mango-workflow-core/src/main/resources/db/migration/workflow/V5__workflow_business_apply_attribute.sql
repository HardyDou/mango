-- 业务申请可筛属性：一行一个键值，供任务分页按业务自定义字段精确过滤。
-- 字段由业务自定义，新增或下线筛选字段都不需要平台改表；多值字段存多行。
-- 写入由业务按申请整体覆盖（先删后插），因此不建唯一键，靠写入路径去重。
CREATE TABLE IF NOT EXISTS `workflow_business_apply_attribute` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL DEFAULT '1' COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `apply_id` bigint NOT NULL COMMENT '业务申请ID',
  `business_type` varchar(128) NOT NULL COMMENT '业务类型，用于按属性侧驱动过滤',
  `attr_key` varchar(64) NOT NULL COMMENT '属性名，由业务自定义',
  `attr_value` varchar(255) NOT NULL COMMENT '属性值，按字面精确匹配',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_apply_attr_apply` (`apply_id`, `attr_key`),
  KEY `idx_workflow_apply_attr_lookup` (`tenant_id`, `business_type`, `attr_key`, `attr_value`(48), `apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务申请可筛属性';
