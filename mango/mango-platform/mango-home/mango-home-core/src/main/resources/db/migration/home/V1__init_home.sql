CREATE TABLE IF NOT EXISTS `sys_user_home_page` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(64) NOT NULL COMMENT '首页名称',
  `layout_json` longtext NOT NULL COMMENT '布局JSON',
  `sort` int NOT NULL DEFAULT '10' COMMENT '排序',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_user_home_page_user` (`tenant_id`, `user_id`, `enabled`, `sort`),
  KEY `idx_sys_user_home_page_org` (`tenant_id`, `org_id`),
  KEY `idx_sys_user_home_page_name` (`tenant_id`, `user_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户首页工作台';

CREATE TABLE IF NOT EXISTS `sys_user_home_preference` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `default_home_page_id` bigint DEFAULT NULL COMMENT '默认首页ID',
  `default_home_ref` varchar(128) DEFAULT NULL COMMENT '默认首页路由标识',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_home_preference_scope` (`tenant_id`, `user_id`),
  KEY `idx_sys_user_home_preference_org` (`tenant_id`, `org_id`),
  KEY `idx_sys_user_home_preference_default` (`default_home_page_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户首页偏好';

CREATE TABLE IF NOT EXISTS `sys_home_template` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `name` varchar(64) NOT NULL COMMENT '模板名称',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `active_version_id` bigint DEFAULT NULL COMMENT '当前发布版本ID',
  `active_version_no` int NOT NULL DEFAULT '0' COMMENT '当前发布版本号',
  `sort` int NOT NULL DEFAULT '10' COMMENT '排序',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_home_template_tenant` (`tenant_id`, `enabled`, `sort`),
  KEY `idx_sys_home_template_org` (`tenant_id`, `org_id`),
  KEY `idx_sys_home_template_name` (`tenant_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页模板';

CREATE TABLE IF NOT EXISTS `sys_home_template_version` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `version_no` int NOT NULL COMMENT '版本号',
  `status` varchar(16) NOT NULL COMMENT '版本状态：DRAFT/ACTIVE/HISTORY',
  `layout_json` longtext NOT NULL COMMENT '布局JSON',
  `source_version_id` bigint DEFAULT NULL COMMENT '来源版本ID',
  `published_by` bigint DEFAULT NULL COMMENT '发布人 ID',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_home_template_version_no` (`template_id`, `version_no`),
  KEY `idx_sys_home_template_version_status` (`tenant_id`, `template_id`, `status`),
  KEY `idx_sys_home_template_version_org` (`tenant_id`, `org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页模板版本';

CREATE TABLE IF NOT EXISTS `sys_home_template_authorization` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `subject_type` varchar(16) NOT NULL COMMENT '授权对象类型：USER/ORG/ROLE',
  `subject_id` bigint NOT NULL DEFAULT '0' COMMENT '授权对象ID',
  `subject_code` varchar(128) NOT NULL DEFAULT '' COMMENT '授权对象编码',
  `subject_name` varchar(128) DEFAULT NULL COMMENT '授权对象名称',
  `default_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认首页候选',
  `sort` int NOT NULL DEFAULT '10' COMMENT '排序',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_home_template_auth_subject` (`tenant_id`, `template_id`, `subject_type`, `subject_id`, `subject_code`),
  KEY `idx_sys_home_template_auth_lookup` (`tenant_id`, `subject_type`, `subject_id`, `subject_code`, `enabled`),
  KEY `idx_sys_home_template_auth_template` (`tenant_id`, `template_id`, `enabled`),
  KEY `idx_sys_home_template_auth_org` (`tenant_id`, `org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页模板授权';
