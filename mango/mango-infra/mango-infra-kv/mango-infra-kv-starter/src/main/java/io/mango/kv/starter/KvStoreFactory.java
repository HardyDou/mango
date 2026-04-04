package io.mango.kv.starter;

import io.mango.kv.api.IKvStore;
import io.mango.kv.api.enums.KvStoreTypeEnum;

/**
 * Factory for creating KV store instances.
 */
public class KvStoreFactory {

    /**
     * Create KV store instance by type.
     * Note: For AUTO type, use CascadingKvStore directly via Spring bean.
     *       This static method is for programmatic creation when Spring context is unavailable.
     */
    public static IKvStore create(KvStoreTypeEnum type) {
        if (type == null || type == KvStoreTypeEnum.AUTO) {
            throw new IllegalStateException("AUTO type requires CascadingKvStore, use Spring bean");
        }
        return switch (type) {
            case REDIS -> throw new IllegalStateException("RedisKvStore requires StringRedisTemplate, use Spring bean");
            case DB -> throw new IllegalStateException("DbKvStore requires JdbcTemplate, use Spring bean");
            case MEMORY -> new io.mango.kv.memory.MemoryKvStore();
            default -> throw new IllegalStateException("Unknown KV type: " + type);
        };
    }
}
