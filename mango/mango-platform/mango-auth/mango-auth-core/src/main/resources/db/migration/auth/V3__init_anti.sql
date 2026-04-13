-- ================================================
-- V3: 防重放/幂等表
-- ================================================

-- 防重放 nonce 记录表
CREATE TABLE IF NOT EXISTS `sys_anti_nonce` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键，雪花算法' PRIMARY KEY,
    `tenant_id`   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `nonce`       VARCHAR(64) NOT NULL COMMENT '防重放nonce',
    `user_id`     BIGINT COMMENT '用户ID（可为空，匿名请求）',
    `path`        VARCHAR(200) NOT NULL COMMENT '请求路径',
    `request_time` BIGINT NOT NULL COMMENT '请求时间戳',
    `expire_time`  DATETIME NOT NULL COMMENT '过期时间',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_nonce` (`nonce`),
    KEY `idx_expire_time` (`expire_time`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='防重放nonce记录表';

-- 幂等记录表（DB fallback 方案）
CREATE TABLE IF NOT EXISTS `sys_idempotency_record` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键，雪花算法' PRIMARY KEY,
    `tenant_id`   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `idempotency_key` VARCHAR(64) NOT NULL COMMENT '幂等Key',
    `user_id`     BIGINT COMMENT '用户ID',
    `path`        VARCHAR(200) NOT NULL COMMENT '请求路径',
    `method`      VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    `response`    TEXT COMMENT '响应体（JSON）',
    `status_code` INT NOT NULL DEFAULT 200 COMMENT 'HTTP状态码',
    `expire_time`  DATETIME NOT NULL COMMENT '过期时间',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
    KEY `idx_expire_time` (`expire_time`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等记录表（DB fallback）';

-- 网关黑白名单表
CREATE TABLE IF NOT EXISTS `sys_gateway_blacklist` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键，雪花算法' PRIMARY KEY,
    `type`        VARCHAR(20) NOT NULL COMMENT '类型：IP / USER_ID / IP_PREFIX',
    `value`       VARCHAR(100) NOT NULL COMMENT '值（IP、用户ID或IP网段）',
    `action`      VARCHAR(10) NOT NULL DEFAULT 'DENY' COMMENT '动作：DENY / ALLOW',
    `reason`      VARCHAR(500) DEFAULT NULL COMMENT '原因',
    `tenant_id`   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `expire_time`  DATETIME DEFAULT NULL COMMENT '过期时间（null=永久）',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`    TINYINT NOT NULL DEFAULT 0,
    KEY `idx_type_value` (`type`, `value`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关黑白名单表';

-- 网关限流规则表
CREATE TABLE IF NOT EXISTS `sys_gateway_rate_limit` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键，雪花算法' PRIMARY KEY,
    `name`        VARCHAR(100) NOT NULL COMMENT '规则名称',
    `limit_type`  VARCHAR(20) NOT NULL COMMENT '限流维度：IP / USER_ID / PATH / IP_USER',
    `limit_key`   VARCHAR(200) NOT NULL COMMENT '限流key（path或*）',
    `rate`        INT NOT NULL COMMENT '速率（次/时间窗口）',
    `interval_sec` INT NOT NULL COMMENT '时间窗口（秒）',
    `burst`       INT NOT NULL DEFAULT 0 COMMENT '突发余量',
    `store_type`  VARCHAR(20) NOT NULL DEFAULT 'auto' COMMENT '存储方案：auto / redis / db / memory',
    `tenant_id`   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`    TINYINT NOT NULL DEFAULT 0,
    KEY `idx_limit_type_key` (`limit_type`, `limit_key`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关限流规则表';

-- 网关限流计数记录（DB fallback）
CREATE TABLE IF NOT EXISTS `sys_gateway_rate_record` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键，雪花算法' PRIMARY KEY,
    `limit_key`   VARCHAR(200) NOT NULL COMMENT '限流key',
    `limit_type`  VARCHAR(20) NOT NULL COMMENT '限流维度',
    `window_start` DATETIME NOT NULL COMMENT '窗口开始时间',
    `count`       INT NOT NULL DEFAULT 0 COMMENT '当前计数',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_limit_window` (`limit_key`, `limit_type`, `window_start`),
    KEY `idx_window_start` (`window_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关限流计数记录（DB fallback）';
