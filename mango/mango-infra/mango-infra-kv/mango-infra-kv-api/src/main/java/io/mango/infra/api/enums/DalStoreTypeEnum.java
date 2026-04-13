package io.mango.infra.kv.api.enums;

/**
 * DAL store type for explicit selection via mango.dal.kvstore.type.
 * When type is not configured (auto), detection order: RedissonClient → DataSource → MemoryKvStore.
 */
public enum DalStoreTypeEnum {
    REDIS,  // Force RedisKvStore (requires RedissonClient)
    DB,     // Force JdbcKvStore (requires DataSource)
    MEMORY  // Force MemoryKvStore (no dependencies)
}
