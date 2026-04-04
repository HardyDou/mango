package io.mango.kv.starter;

import io.mango.kv.api.IKvStore;
import io.mango.kv.api.enums.KvStoreTypeEnum;
import io.mango.kv.db.DbKvStore;
import io.mango.kv.memory.MemoryKvStore;
import io.mango.kv.redis.RedisKvStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * KV Store auto-configuration.
 * Creates IKvStore bean based on mango.kv.store-type property.
 */
@Slf4j
@Configuration
public class KvStoreAutoConfig {

    /**
     * Properties for KV store configuration.
     */
    @Bean
    @ConditionalOnMissingBean
    public KvStoreProperties kvStoreProperties() {
        return new KvStoreProperties();
    }

    /**
     * KV Store factory for creating instances.
     */
    @Bean
    @ConditionalOnMissingBean(IKvStore.class)
    @ConditionalOnProperty(prefix = "mango.kv", name = "store-type", havingValue = "memory", matchIfMissing = true)
    public IKvStore memoryKvStore() {
        log.info("KV Store initialized: MemoryKvStore");
        return new MemoryKvStore();
    }

    @Bean
    @ConditionalOnMissingBean(IKvStore.class)
    @ConditionalOnProperty(prefix = "mango.kv", name = "store-type", havingValue = "redis", matchIfMissing = true)
    public IKvStore redisKvStore(StringRedisTemplate redisTemplate) {
        log.info("KV Store initialized: RedisKvStore");
        return new RedisKvStore(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(IKvStore.class)
    @ConditionalOnProperty(prefix = "mango.kv", name = "store-type", havingValue = "db", matchIfMissing = true)
    public IKvStore dbKvStore(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                               StringRedisTemplate stringRedisTemplate) {
        log.info("KV Store initialized: DbKvStore");
        return new DbKvStore(jdbcTemplate, stringRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(IKvStore.class)
    @ConditionalOnProperty(prefix = "mango.kv", name = "store-type", havingValue = "auto", matchIfMissing = true)
    public IKvStore cascadingKvStore(
            StringRedisTemplate redisTemplate,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        log.info("KV Store initialized: CascadingKvStore (Redis → DB → Memory)");
        return new CascadingKvStore(redisTemplate, jdbcTemplate);
    }
}
