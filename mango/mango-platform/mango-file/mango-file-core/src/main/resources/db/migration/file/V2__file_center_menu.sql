INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(28,1,'internal-admin',0,1,'文件中心','file','/file','FolderOpened',NULL,3,1,1,0,0,'/file/files',NULL,NULL,NULL,NOW(),NOW(),'统一文件上传、管理、预览与存储配置入口',0,NULL,NOW(),NULL,NOW())
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
`redirect` = VALUES(`redirect`),
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`),
`update_time` = NOW(),
`updated_at` = NOW();

UPDATE `authorization_menu`
SET `parent_id` = 28,
    `menu_type` = 2,
    `menu_name` = '文件管理',
    `menu_code` = 'file:files',
    `path` = '/file/files',
    `icon` = 'Files',
    `component` = '@/views/file/files/index.vue',
    `sort` = 1,
    `status` = 1,
    `visible` = 1,
    `redirect` = NULL,
    `permissions` = 'file:files:list',
    `remark` = '文件记录、上传下载、预览和归档管理',
    `del_flag` = 0,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22;

UPDATE `authorization_menu`
SET `parent_id` = 28,
    `menu_type` = 2,
    `menu_name` = '存储配置',
    `menu_code` = 'file:storage-configs',
    `path` = '/file/storage-configs',
    `icon` = 'Setting',
    `component` = '@/views/file/storage-configs/index.vue',
    `sort` = 2,
    `status` = 1,
    `visible` = 1,
    `redirect` = NULL,
    `permissions` = 'file:storage-configs:list',
    `remark` = '文件底层存储配置管理',
    `del_flag` = 0,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 23;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:list',
    `permissions` = 'file:files:list',
    `icon` = 'Search',
    `remark` = '文件记录列表查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22001;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:query',
    `permissions` = 'file:files:query',
    `icon` = 'View',
    `remark` = '文件详情与预览元数据查询权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22002;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:upload',
    `permissions` = 'file:files:upload',
    `icon` = 'Upload',
    `remark` = '文件中心上传权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22003;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:download',
    `permissions` = 'file:files:download',
    `icon` = 'Download',
    `remark` = '文件下载权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22004;

UPDATE `authorization_menu`
SET `menu_code` = 'file:files:archive',
    `permissions` = 'file:files:archive',
    `icon` = 'Delete',
    `remark` = '文件归档权限',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 22005;

INSERT INTO `authorization_menu` (`id`, `tenant_id`, `app_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_by`, `update_by`, `create_time`, `update_time`, `remark`, `del_flag`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(22006,1,'internal-admin',22,3,'目录查询','file:directories:list',NULL,'FolderSearch',NULL,6,1,0,0,0,NULL,'file:directories:list',NULL,NULL,NOW(),NOW(),'文件目录树查询权限',0,NULL,NOW(),NULL,NOW()),
(22007,1,'internal-admin',22,3,'目录新增','file:directories:add',NULL,'FolderPlus',NULL,7,1,0,0,0,NULL,'file:directories:add',NULL,NULL,NOW(),NOW(),'文件目录新增权限',0,NULL,NOW(),NULL,NOW()),
(22008,1,'internal-admin',22,3,'目录编辑','file:directories:edit',NULL,'Edit',NULL,8,1,0,0,0,NULL,'file:directories:edit',NULL,NULL,NOW(),NOW(),'文件目录编辑权限',0,NULL,NOW(),NULL,NOW()),
(22009,1,'internal-admin',22,3,'目录删除','file:directories:delete',NULL,'FolderDelete',NULL,9,1,0,0,0,NULL,'file:directories:delete',NULL,NULL,NOW(),NOW(),'文件目录删除权限',0,NULL,NOW(),NULL,NOW())
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`menu_type` = VALUES(`menu_type`),
`menu_name` = VALUES(`menu_name`),
`menu_code` = VALUES(`menu_code`),
`icon` = VALUES(`icon`),
`sort` = VALUES(`sort`),
`status` = VALUES(`status`),
`visible` = VALUES(`visible`),
`permissions` = VALUES(`permissions`),
`remark` = VALUES(`remark`),
`del_flag` = VALUES(`del_flag`),
`update_time` = NOW(),
`updated_at` = NOW();

UPDATE `authorization_menu`
SET `menu_code` = REPLACE(`menu_code`, 'system:file-storage', 'file:storage-configs'),
    `permissions` = REPLACE(`permissions`, 'system:file-storage', 'file:storage-configs'),
    `icon` = CASE `id`
        WHEN 23001 THEN 'Search'
        WHEN 23002 THEN 'View'
        WHEN 23003 THEN 'CirclePlus'
        WHEN 23004 THEN 'Edit'
        WHEN 23005 THEN 'Delete'
        WHEN 23006 THEN 'Connection'
        WHEN 23007 THEN 'CircleCheck'
        ELSE `icon`
    END,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` BETWEEN 23001 AND 23007;

INSERT INTO `file_storage_config` (`id`, `tenant_id`, `config_name`, `storage_type`, `endpoint`, `public_endpoint`, `region`, `bucket_name`, `access_key`, `secret_key`, `path_style_access`, `ssl_enabled`, `active`, `status`, `remark`, `created_by`, `created_time`, `created_at`, `updated_by`, `updated_time`, `updated_at`)
VALUES
(2,1,'MinIO 本地联调','MINIO','http://127.0.0.1:9000','http://file.mango.io:9000','us-east-1','mango-file','minioadmin','minioadmin',1,0,0,1,'本地 Docker Compose MinIO 示例配置。启动 mango-file/docker-compose.yml 后可设为默认验证对象存储。',NULL,NOW(),NOW(),NULL,NOW(),NOW())
ON DUPLICATE KEY UPDATE
`storage_type` = VALUES(`storage_type`),
`endpoint` = VALUES(`endpoint`),
`public_endpoint` = VALUES(`public_endpoint`),
`region` = VALUES(`region`),
`bucket_name` = VALUES(`bucket_name`),
`access_key` = VALUES(`access_key`),
`secret_key` = VALUES(`secret_key`),
`path_style_access` = VALUES(`path_style_access`),
`ssl_enabled` = VALUES(`ssl_enabled`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`updated_time` = NOW(),
`updated_at` = NOW();

INSERT IGNORE INTO `authorization_menu_package_item` (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`) VALUES
(1097,1,1,28,97),
(1098,1,1,22,98),
(1099,1,1,23,99),
(1100,1,1,22001,100),
(1101,1,1,22002,101),
(1102,1,1,22003,102),
(1103,1,1,22004,103),
(1104,1,1,22005,104),
(1105,1,1,22006,105),
(1106,1,1,22007,106),
(1107,1,1,22008,107),
(1108,1,1,22009,108),
(1109,1,1,23001,109),
(1110,1,1,23002,110),
(1111,1,1,23003,111),
(1115,1,1,23004,112),
(1116,1,1,23005,113),
(1117,1,1,23006,114),
(1118,1,1,23007,115);

INSERT IGNORE INTO `authorization_role_menu` (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
SELECT 500000 + `menu_id`, 1, 1, `menu_id`, NOW(), NULL, NOW(), NULL, NOW()
FROM `authorization_menu_package_item`
WHERE `package_id` = 1
  AND `menu_id` IN (28,22,23,22001,22002,22003,22004,22005,22006,22007,22008,22009,23001,23002,23003,23004,23005,23006,23007);
