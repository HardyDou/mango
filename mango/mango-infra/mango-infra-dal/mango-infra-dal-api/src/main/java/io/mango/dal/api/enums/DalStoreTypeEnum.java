package io.mango.dal.api.enums;

/**
 * DAL store type for explicit selection via mango.dal.kvstore.type.
 * When type is not configured (auto), detection order: RedissonClient → DataSource → MemoryXivStore.
 */
public enum DalStoreTypeEnum {
    REDIS,  // Force RedisXivStore (requires RedissonClient)
    DB,     // Force DbXivStore (requires DataSource)
    MEMORY  // Force MemoryXivStore (no dependencies)
}
