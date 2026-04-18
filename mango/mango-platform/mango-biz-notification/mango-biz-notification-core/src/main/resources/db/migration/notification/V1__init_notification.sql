CREATE TABLE IF NOT EXISTS `sys_notification` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
    `notification_type` VARCHAR(20) NOT NULL COMMENT '通知类型: SYSTEM/BUSINESS/ALERT/CHAT',
    `title`       VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content`     TEXT NOT NULL COMMENT '通知内容',
    `user_id`     BIGINT DEFAULT NULL COMMENT '接收用户ID (null=广播)',
    `priority`    TINYINT NOT NULL DEFAULT 0 COMMENT '优先级: 0-低 1-中 2-高',
    `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态: 0-未读 1-已读',
    `read_time`   DATETIME DEFAULT NULL COMMENT '阅读时间',
    `create_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_read_status` (`read_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
