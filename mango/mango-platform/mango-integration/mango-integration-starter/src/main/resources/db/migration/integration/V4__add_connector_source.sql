ALTER TABLE `integration_connector`
    ADD COLUMN `source` varchar(16) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL-页面维护，DECLARED-资源声明' AFTER `status`;
