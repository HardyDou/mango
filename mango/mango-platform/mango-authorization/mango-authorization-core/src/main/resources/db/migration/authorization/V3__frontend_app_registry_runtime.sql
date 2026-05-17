CREATE TABLE IF NOT EXISTS `frontend_app_registry` (
  `id` bigint NOT NULL COMMENT '前端入口注册ID',
  `app_code` varchar(64) NOT NULL COMMENT '授权应用编码',
  `app_type` varchar(32) NOT NULL DEFAULT 'LOCAL' COMMENT '前端入口类型: LOCAL/MICRO_APP/IFRAME/EXTERNAL_LINK',
  `deploy_mode` varchar(32) NOT NULL DEFAULT 'EMBEDDED' COMMENT '部署模式: EMBEDDED/REMOTE/HYBRID',
  `entry_url` varchar(500) DEFAULT NULL COMMENT '远程入口地址',
  `mount_path` varchar(255) DEFAULT NULL COMMENT '主框架挂载路径',
  `active_rule` varchar(255) DEFAULT NULL COMMENT '激活规则',
  `framework` varchar(64) DEFAULT NULL COMMENT '前端运行框架',
  `version` varchar(64) DEFAULT NULL COMMENT '当前版本',
  `health_check_url` varchar(500) DEFAULT NULL COMMENT '健康检查地址',
  `sandbox_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用沙箱',
  `style_isolation` varchar(32) NOT NULL DEFAULT 'NONE' COMMENT '样式隔离: NONE/SCOPED/SHADOW_DOM/IFRAME',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_frontend_app_registry_app_code` (`app_code`),
  KEY `idx_frontend_app_registry_type` (`app_type`),
  KEY `idx_frontend_app_registry_mount_path` (`mount_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端应用入口注册表';

INSERT IGNORE INTO `frontend_app_registry`
  (`id`, `app_code`, `app_type`, `deploy_mode`, `framework`, `sandbox_enabled`, `style_isolation`, `create_time`, `update_time`)
VALUES
  (1, 'internal-admin', 'LOCAL', 'EMBEDDED', 'vue3', 0, 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE IF NOT EXISTS `frontend_menu_runtime_config` (
  `id` bigint NOT NULL COMMENT '菜单运行配置ID',
  `menu_id` bigint NOT NULL COMMENT '授权菜单ID',
  `app_code` varchar(64) NOT NULL COMMENT '授权应用编码',
  `page_type` varchar(32) NOT NULL DEFAULT 'LOCAL_ROUTE' COMMENT '页面运行类型: LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK/BUTTON',
  `external_url` varchar(500) DEFAULT NULL COMMENT 'iframe 或外链地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_frontend_menu_runtime_menu_id` (`menu_id`),
  KEY `idx_frontend_menu_runtime_app_code` (`app_code`),
  KEY `idx_frontend_menu_runtime_page_type` (`page_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端菜单运行配置表';

INSERT IGNORE INTO `frontend_menu_runtime_config`
  (`id`, `menu_id`, `app_code`, `page_type`, `create_time`, `update_time`)
SELECT `id`, `id`, `app_code`,
       CASE
         WHEN `menu_type` = 3 THEN 'BUTTON'
         WHEN `embedded` = 1 THEN 'IFRAME'
         ELSE 'LOCAL_ROUTE'
       END,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM `authorization_menu`;

CREATE TABLE IF NOT EXISTS `frontend_tenant_app_binding` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户 ID',
  `app_code` varchar(64) NOT NULL COMMENT '前端入口所属授权应用编码',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_frontend_tenant_app_binding` (`tenant_id`,`app_code`),
  KEY `idx_frontend_tenant_app_binding_app_code` (`app_code`),
  KEY `idx_frontend_tenant_app_binding_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端租户应用开通关系表';

INSERT IGNORE INTO `frontend_tenant_app_binding`
  (`id`, `tenant_id`, `app_code`, `status`, `create_time`, `update_time`)
SELECT `tenant_id`, `tenant_id`, 'internal-admin', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM `authorization_role`
WHERE `app_code` = 'internal-admin'
GROUP BY `tenant_id`;
