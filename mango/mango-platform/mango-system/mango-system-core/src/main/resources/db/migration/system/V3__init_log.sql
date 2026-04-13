CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
    `user_id`     BIGINT DEFAULT NULL COMMENT '用户ID',
    `username`    VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    `login_type`  VARCHAR(20) DEFAULT NULL COMMENT '登录类型',
    `ip`          VARCHAR(128) DEFAULT NULL COMMENT '登录IP',
    `location`    VARCHAR(255) DEFAULT NULL COMMENT '登录地点',
    `browser`     VARCHAR(128) DEFAULT NULL COMMENT '浏览器',
    `os`          VARCHAR(64) DEFAULT NULL COMMENT '操作系统',
    `status`      TINYINT DEFAULT NULL COMMENT '登录状态',
    `msg`         VARCHAR(500) DEFAULT NULL COMMENT '提示消息',
    `login_time`  DATETIME DEFAULT NULL COMMENT '登录时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
    `user_id`     BIGINT DEFAULT NULL COMMENT '用户ID',
    `username`    VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    `module`      VARCHAR(64) DEFAULT NULL COMMENT '操作模块',
    `operation`   VARCHAR(100) DEFAULT NULL COMMENT '操作名称',
    `method`      VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `url`         VARCHAR(500) DEFAULT NULL COMMENT '请求路径',
    `params`      TEXT COMMENT '请求参数',
    `result`      TEXT COMMENT '请求结果',
    `status`      TINYINT DEFAULT NULL COMMENT '操作状态',
    `error_msg`   VARCHAR(500) DEFAULT NULL COMMENT '错误消息',
    `duration`    BIGINT DEFAULT NULL COMMENT '执行时间(ms)',
    `ip`          VARCHAR(128) DEFAULT NULL COMMENT '操作IP',
    `operate_time` DATETIME DEFAULT NULL COMMENT '操作时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
