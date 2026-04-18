package io.mango.infra.kv.api.enums;

/**
 * KV store type for explicit selection via mango.kv.store.type.
 * When type is auto, detection order is RedissonClient then MemoryKvStore.
 */
public enum KvStoreTypeEnum {
    AUTO,   // Auto-detect RedisKvStore, then MemoryKvStore
    REDIS,  // Force RedisKvStore (requires RedissonClient)
    JDBC,   // Force JdbcKvStore (requires JdbcTemplate and RedissonClient)
    DB,     // Legacy alias of JDBC
    MEMORY  // Force MemoryKvStore (no dependencies)
}
