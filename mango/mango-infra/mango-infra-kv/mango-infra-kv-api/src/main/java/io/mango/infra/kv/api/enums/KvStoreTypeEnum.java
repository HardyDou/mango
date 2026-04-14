package io.mango.infra.kv.api.enums;

/**
 * KV store type for explicit selection via mango.kv.type.
 * When type is not configured (auto), detection order: RedissonClient → DataSource → MemoryKvStore.
 */
public enum KvStoreTypeEnum {
    REDIS,  // Force RedisKvStore (requires RedissonClient)
    DB,     // Force JdbcKvStore (requires DataSource)
    MEMORY  // Force MemoryKvStore (no dependencies)
}
