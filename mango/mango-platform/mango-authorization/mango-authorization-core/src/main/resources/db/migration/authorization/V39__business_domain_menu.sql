-- Register business domain menu under system settings.

SET @domain_module_id := (
  SELECT `id`
  FROM `authorization_app_module`
  WHERE `app_code` = 'internal-admin'
    AND `module_code` = 'mango-domain'
  LIMIT 1
);

SET @domain_module_id := COALESCE(@domain_module_id, (
  SELECT COALESCE(MAX(`id`), 0) + 1
  FROM `authorization_app_module`
));

INSERT INTO `authorization_app_module`
  (`id`, `app_code`, `module_code`, `module_name`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (@domain_module_id, 'internal-admin', 'mango-domain', '业务域模块', 1, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `module_name` = VALUES(`module_name`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `frontend_module_runtime_strategy`
  (`id`, `app_code`, `module_code`, `deploy_profile`, `page_type`, `runtime_code`, `status`, `sort`, `create_time`, `update_time`)
VALUES
  (9, 'internal-admin', 'mango-domain', 'monolith', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19, 'internal-admin', 'mango-domain', 'hybrid', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (29, 'internal-admin', 'mango-domain', 'micro', 'LOCAL_ROUTE', 'mango-admin-local', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `page_type` = VALUES(`page_type`),
  `runtime_code` = VALUES(`runtime_code`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;

SET @system_parent_id := (
  SELECT `id`
  FROM `authorization_menu`
  WHERE `app_code` = 'internal-admin'
    AND `menu_code` = 'system'
  LIMIT 1
);

INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `path`, `icon`, `component`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `redirect`, `permissions`, `create_time`, `update_time`, `remark`, `del_flag`, `created_at`, `updated_at`)
VALUES
  (2400, 1, 'internal-admin', 'mango-domain', @system_parent_id, 2, '业务域', 'domain', '/system/domain', 'Grid', '@/views/system/domain/index.vue', 8, 1, 1, 0, 0, NULL, 'domain:list', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '业务域编码、前缀、层级和状态管理', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `module_code` = VALUES(`module_code`),
  `menu_name` = VALUES(`menu_name`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `permissions` = VALUES(`permissions`),
  `remark` = VALUES(`remark`),
  `update_time` = CURRENT_TIMESTAMP,
  `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `authorization_menu`
  (`id`, `tenant_id`, `app_code`, `module_code`, `parent_id`, `menu_type`, `menu_name`, `menu_code`, `sort`, `status`, `visible`, `keep_alive`, `embedded`, `permissions`, `create_time`, `update_time`, `remark`, `del_flag`, `created_at`, `updated_at`)
VALUES
  (240001, 1, 'internal-admin', 'mango-domain', 2400, 3, '查询业务域', 'domain:list', 1, 1, 0, 0, 0, 'domain:list', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '业务域列表和详情查询权限', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (240002, 1, 'internal-admin', 'mango-domain', 2400, 3, '新增业务域', 'domain:add', 2, 1, 0, 0, 0, 'domain:add', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '业务域新增权限', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (240003, 1, 'internal-admin', 'mango-domain', 2400, 3, '编辑业务域', 'domain:edit', 3, 1, 0, 0, 0, 'domain:edit', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '业务域编辑权限', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (240004, 1, 'internal-admin', 'mango-domain', 2400, 3, '删除业务域', 'domain:delete', 4, 1, 0, 0, 0, 'domain:delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '业务域删除权限', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (240005, 1, 'internal-admin', 'mango-domain', 2400, 3, '启停业务域', 'domain:status', 5, 1, 0, 0, 0, 'domain:status', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '业务域启停权限', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `module_code` = VALUES(`module_code`),
  `menu_name` = VALUES(`menu_name`),
  `permissions` = VALUES(`permissions`),
  `remark` = VALUES(`remark`),
  `update_time` = CURRENT_TIMESTAMP,
  `updated_at` = CURRENT_TIMESTAMP;

INSERT IGNORE INTO `authorization_menu_package_item`
  (`id`, `tenant_id`, `package_id`, `menu_id`, `sort`)
VALUES
  (12400, 1, 1, 2400, 80),
  (22400, 1, 2, 2400, 80);

INSERT IGNORE INTO `authorization_role_menu`
  (`id`, `tenant_id`, `role_id`, `menu_id`, `create_time`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (52400, 1, 1, 2400, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (52401, 1, 1, 240001, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (52402, 1, 1, 240002, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (52403, 1, 1, 240003, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (52404, 1, 1, 240004, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (52405, 1, 1, 240005, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (62400, 1, 2, 2400, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (62401, 1, 2, 240001, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (62402, 1, 2, 240002, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (62403, 1, 2, 240003, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (62404, 1, 2, 240004, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (62405, 1, 2, 240005, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP);

INSERT INTO `frontend_menu_runtime_config`
  (`id`, `menu_id`, `app_code`, `page_type`, `external_url`, `create_time`, `update_time`)
SELECT `menu`.`id`,
       `menu`.`id`,
       `menu`.`app_code`,
       CASE
         WHEN `menu`.`menu_type` = 3 THEN 'BUTTON'
         WHEN `menu`.`embedded` = 1 THEN 'IFRAME'
         ELSE 'LOCAL_ROUTE'
       END,
       NULL,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM `authorization_menu` `menu`
WHERE `menu`.`id` IN (2400, 240001, 240002, 240003, 240004, 240005)
ON DUPLICATE KEY UPDATE
  `app_code` = VALUES(`app_code`),
  `page_type` = VALUES(`page_type`),
  `external_url` = VALUES(`external_url`),
  `update_time` = CURRENT_TIMESTAMP;
