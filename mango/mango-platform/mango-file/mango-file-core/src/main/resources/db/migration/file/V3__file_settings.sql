CREATE TABLE IF NOT EXISTS `file_settings` (
  `id` bigint NOT NULL COMMENT '文件中心配置ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '机构隔离ID，用户可见语义为机构，技术字段保留tenant_id',
  `max_size` bigint NOT NULL DEFAULT '104857600' COMMENT '单文件最大大小，单位字节',
  `allowed_extensions` varchar(1000) DEFAULT NULL COMMENT '允许上传扩展名，逗号分隔；为空表示不限制',
  `blocked_extensions` varchar(1000) DEFAULT 'exe,bat,cmd,sh,jar' COMMENT '禁止上传扩展名，逗号分隔',
  `instant_upload_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用秒传: 0-否 1-是',
  `direct_upload_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用客户端直传对象存储: 0-否 1-是',
  `direct_upload_expire_seconds` bigint NOT NULL DEFAULT '900' COMMENT '直传签名有效期，单位秒',
  `access_token_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用限时访问令牌: 0-否 1-是',
  `access_token_expire_seconds` bigint NOT NULL DEFAULT '600' COMMENT '访问令牌有效期，单位秒',
  `preview_provider_url` varchar(500) DEFAULT NULL COMMENT '外部文档预览服务地址',
  `preview_expire_seconds` bigint NOT NULL DEFAULT '600' COMMENT '预览访问有效期，单位秒',
  `preview_external_extensions` varchar(1000) DEFAULT 'doc,docx,xls,xlsx,xlsm,ppt,pptx,odt,ods,odp,ofd,wps,et,dps,csv,txt,zip,rar,7z,eml,msg' COMMENT '外部预览扩展名，逗号分隔',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标准创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标准更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_settings_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件中心运行时配置表';

INSERT INTO `file_settings` (`id`, `tenant_id`, `max_size`, `allowed_extensions`, `blocked_extensions`, `instant_upload_enabled`, `direct_upload_enabled`, `direct_upload_expire_seconds`, `access_token_enabled`, `access_token_expire_seconds`, `preview_provider_url`, `preview_expire_seconds`, `preview_external_extensions`, `created_by`, `created_time`, `created_at`, `updated_by`, `updated_time`, `updated_at`)
VALUES
(1,1,104857600,NULL,'exe,bat,cmd,sh,jar',1,0,900,0,600,NULL,600,'doc,docx,xls,xlsx,xlsm,ppt,pptx,odt,ods,odp,ofd,wps,et,dps,csv,txt,zip,rar,7z,eml,msg',NULL,NOW(),NOW(),NULL,NOW(),NOW())
ON DUPLICATE KEY UPDATE
`updated_time` = `updated_time`;

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(24,1,'internal-admin',28,2,'文件配置','file:settings','/file/settings','Tools','@/views/file/settings/index.vue',3,1,1,0,0,NULL,'file:settings:query',NULL,NULL,NOW(),NOW(),'文件上传策略、访问策略、预览策略和直传策略配置',0,NULL,NOW(),NULL,NOW()),
(24001,1,'internal-admin',24,3,'查询','file:settings:query',NULL,'Search',NULL,1,1,0,0,0,NULL,'file:settings:query',NULL,NULL,NOW(),NOW(),'文件中心配置查询权限',0,NULL,NOW(),NULL,NOW()),
(24002,1,'internal-admin',24,3,'编辑','file:settings:edit',NULL,'Edit',NULL,2,1,0,0,0,NULL,'file:settings:edit',NULL,NULL,NOW(),NOW(),'文件中心配置编辑权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`menu_type` = VALUES(`menu_type`),
`menu_name` = VALUES(`menu_name`),
`menu_code` = VALUES(`menu_code`),
`path` = VALUES(`path`),
`icon` = VALUES(`icon`),
`component` = VALUES(`component`),
`sort` = VALUES(`sort`),
`status` = VALUES(`status`),
`visible` = VALUES(`visible`),
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`),
`update_time` = NOW(),
`updated_at` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1112,1,1,24,112),
(1113,1,1,24001,113),
(1114,1,1,24002,114);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 500000 + `menu_id`, 1, 1, `menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu_package_item`
WHERE `package_id` = 1
  AND `menu_id` IN (24,24001,24002);
