ALTER TABLE `file_settings`
    MODIFY COLUMN `duplicate_name_strategy` varchar(32) NOT NULL DEFAULT 'AUTO_RENAME'
    COMMENT '重名处理策略: REJECT AUTO_RENAME ALLOW';
