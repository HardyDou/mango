package io.mango.infra.kv.api.enums;

/**
 * KV store type for explicit selection via mango.kv.store.type.
 * When type is auto, detection order is RedissonClient then MemoryKvStore.
 */
public enum KvStoreTypeEnum {
    /** Auto-detect RedisKvStore first, then fall back to MemoryKvStore. */
    AUTO,
    /** Force RedisKvStore and require RedissonClient. */
    REDIS,
    /** Force JdbcKvStore and require JdbcTemplate. */
    JDBC,
    /** Legacy alias of JDBC for backward compatibility. */
    DB,
    /** Force MemoryKvStore and require no external dependency. */
    MEMORY
}
