CREATE TABLE IF NOT EXISTS `sys_kv_record` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
    `kv_key`      VARCHAR(200) NOT NULL COMMENT 'KV key',
    `kv_value`    TEXT COMMENT 'KV value',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_kv_key` (`kv_key`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KV存储表（防重放/幂等/限流 fallback）';
