ALTER TABLE `authorization_api_resource`
    ADD COLUMN `source` varchar(32) NOT NULL DEFAULT 'FIXED_DECLARATION' COMMENT '登记来源: FIXED_DECLARATION/CONTROLLER_SCAN/MANUAL' AFTER `module_name`,
    ADD COLUMN `domain` varchar(128) NOT NULL DEFAULT 'root' COMMENT '接口一级路径段所属领域' AFTER `source`;

CREATE INDEX `idx_authorization_api_resource_source`
    ON `authorization_api_resource` (`source`);

CREATE INDEX `idx_authorization_api_resource_domain`
    ON `authorization_api_resource` (`domain`);
