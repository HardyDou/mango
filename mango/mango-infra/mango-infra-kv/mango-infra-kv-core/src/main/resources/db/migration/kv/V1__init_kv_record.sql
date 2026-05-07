-- mango-check: disable persistence-audit-fields reason=基础设施运行态 KV 存储，不属于业务主数据
CREATE TABLE IF NOT EXISTS `infra_kv_entry` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
    `kv_key`      VARCHAR(200) NOT NULL COMMENT 'KV key',
    `kv_value`    TEXT COMMENT 'KV value',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_kv_key` (`kv_key`),
    KEY `idx_kv_record_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KV存储表（防重放/幂等/限流 fallback）';
