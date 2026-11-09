ALTER TABLE `integration_connector`
    ADD COLUMN `revision` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `source`;

ALTER TABLE `integration_credential`
    ADD COLUMN `revision` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `status`;

ALTER TABLE `integration_instance`
    ADD COLUMN `revision` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `status`;
