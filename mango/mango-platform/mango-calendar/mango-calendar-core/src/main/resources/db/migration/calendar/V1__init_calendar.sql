CREATE TABLE IF NOT EXISTS `calendar` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `calendar_code` varchar(64) NOT NULL COMMENT '日历编码',
  `calendar_name` varchar(128) NOT NULL COMMENT '日历名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calendar_tenant_code` (`tenant_id`, `calendar_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作日历';

CREATE TABLE IF NOT EXISTS `calendar_day` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `calendar_id` bigint NOT NULL COMMENT '日历ID',
  `calendar_year` int NOT NULL COMMENT '年度',
  `calendar_date` date NOT NULL COMMENT '日期',
  `day_of_week` tinyint NOT NULL COMMENT '星期：1-周一，7-周日',
  `day_type` varchar(32) NOT NULL COMMENT '日期类型',
  `workday` tinyint NOT NULL COMMENT '是否工作日：1-工作日，0-非工作日',
  `day_name` varchar(128) DEFAULT NULL COMMENT '日期名称',
  `source` varchar(64) DEFAULT NULL COMMENT '数据来源',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '启用状态：1-启用，0-停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calendar_day_date` (`tenant_id`, `calendar_id`, `calendar_date`),
  KEY `idx_calendar_day_year_enabled` (`tenant_id`, `calendar_id`, `calendar_year`, `enabled`),
  KEY `idx_calendar_day_workday_date` (`tenant_id`, `calendar_id`, `enabled`, `workday`, `calendar_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作日历年度日期';
