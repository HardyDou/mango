-- Calendar schema baseline. Flyway owns DDL only; required and demo data are
-- registered from the calendar starter resource manifests.

CREATE TABLE IF NOT EXISTS `calendar` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户标识',
  `org_id` bigint DEFAULT NULL COMMENT '组织标识',
  `calendar_code` varchar(64) NOT NULL COMMENT '日历编码',
  `calendar_name` varchar(128) NOT NULL COMMENT '日历名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-停用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calendar_tenant_code` (`tenant_id`, `calendar_code`),
  KEY `idx_calendar_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作日历';

CREATE TABLE IF NOT EXISTS `calendar_day` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户标识',
  `org_id` bigint DEFAULT NULL COMMENT '组织标识',
  `calendar_id` bigint NOT NULL COMMENT '日历 ID',
  `calendar_year` int NOT NULL COMMENT '年度',
  `calendar_date` date NOT NULL COMMENT '日期',
  `day_of_week` tinyint NOT NULL COMMENT '星期：1-周一，7-周日',
  `day_type` varchar(32) NOT NULL COMMENT '日期类型',
  `workday` tinyint NOT NULL COMMENT '是否工作日：1-工作日，0-非工作日',
  `day_name` varchar(128) DEFAULT NULL COMMENT '日期名称',
  `lunar_year` int DEFAULT NULL COMMENT '农历年',
  `lunar_month` tinyint DEFAULT NULL COMMENT '农历月',
  `lunar_day` tinyint DEFAULT NULL COMMENT '农历日',
  `lunar_leap_month` tinyint NOT NULL DEFAULT '0' COMMENT '是否农历闰月：1-是，0-否',
  `lunar_text` varchar(32) DEFAULT NULL COMMENT '农历中文日期',
  `ganzhi_year` varchar(16) DEFAULT NULL COMMENT '干支纪年',
  `zodiac` varchar(8) DEFAULT NULL COMMENT '生肖',
  `solar_term` varchar(16) DEFAULT NULL COMMENT '节气',
  `source` varchar(64) DEFAULT NULL COMMENT '数据来源',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '启用状态：1-启用，0-停用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calendar_day_date` (`tenant_id`, `calendar_id`, `calendar_date`),
  KEY `idx_calendar_day_year_enabled` (`tenant_id`, `calendar_id`, `calendar_year`, `enabled`),
  KEY `idx_calendar_day_workday_date` (`tenant_id`, `calendar_id`, `enabled`, `workday`, `calendar_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作日历年度日期';
