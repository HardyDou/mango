-- 人工接管改为同一行上的覆盖标记，不再复制记录、不再伪造 manual-control 模块。
ALTER TABLE `authorization_api_resource`
    ADD COLUMN `manual_override` tinyint NOT NULL DEFAULT 0 COMMENT '是否人工接管：0-否 1-是' AFTER `source`,
    ADD COLUMN `origin_source` varchar(32) DEFAULT NULL COMMENT '接管前的登记来源' AFTER `manual_override`,
    ADD COLUMN `origin_access_mode` varchar(32) DEFAULT NULL COMMENT '接管前的访问模式' AFTER `origin_source`,
    ADD COLUMN `origin_permission_code` varchar(255) DEFAULT NULL COMMENT '接管前的权限标识' AFTER `origin_access_mode`,
    ADD COLUMN `origin_description` varchar(255) DEFAULT NULL COMMENT '接管前的资源描述' AFTER `origin_permission_code`;

CREATE INDEX `idx_authorization_api_resource_manual_override`
    ON `authorization_api_resource` (`manual_override`);

-- 历史数据归并：把上一版 manual-control 接管行合并回同路由的声明行。
UPDATE `authorization_api_resource` AS `declaration`
    JOIN `authorization_api_resource` AS `manual`
    ON `manual`.`module_name` = 'manual-control'
        AND `manual`.`source` = 'MANUAL'
        AND `manual`.`status` = 1
        AND `manual`.`path_pattern` = `declaration`.`path_pattern`
        AND `manual`.`http_method` = `declaration`.`http_method`
        AND `declaration`.`module_name` <> 'manual-control'
        AND `declaration`.`status` = 1
SET
    `declaration`.`origin_source` = `declaration`.`source`,
    `declaration`.`origin_access_mode` = `declaration`.`access_mode`,
    `declaration`.`origin_permission_code` = `declaration`.`permission_code`,
    `declaration`.`origin_description` = `declaration`.`description`,
    `declaration`.`source` = 'MANUAL',
    `declaration`.`access_mode` = `manual`.`access_mode`,
    `declaration`.`permission_code` = `manual`.`permission_code`,
    `declaration`.`description` = `manual`.`description`,
    `declaration`.`manual_override` = 1;

-- 已合并的历史接管行删除；没有对应声明行的接管行转成独占接管，释放时物理删除。
DELETE FROM `authorization_api_resource` AS `manual`
    WHERE `manual`.`module_name` = 'manual-control'
      AND `manual`.`source` = 'MANUAL'
      AND `manual`.`status` = 1
      AND EXISTS (
          SELECT 1 FROM (
              SELECT `path_pattern`, `http_method`
              FROM `authorization_api_resource`
              WHERE `module_name` <> 'manual-control' AND `status` = 1
          ) AS `declaration`
          WHERE `declaration`.`path_pattern` = `manual`.`path_pattern`
            AND `declaration`.`http_method` = `manual`.`http_method`
      );

UPDATE `authorization_api_resource`
SET `manual_override` = 1,
    `origin_access_mode` = `access_mode`,
    `origin_permission_code` = `permission_code`,
    `origin_description` = `description`
WHERE `module_name` = 'manual-control' AND `source` = 'MANUAL' AND `status` = 1;
