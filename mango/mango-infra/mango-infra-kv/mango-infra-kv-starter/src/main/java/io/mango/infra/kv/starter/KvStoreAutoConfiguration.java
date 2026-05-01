package io.mango.infra.kv.starter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.infra.kv.core.redis.RedisKvStore;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * KV store auto-configuration.
 *
 * Creates one IKvStore bean based on mango.kv.store.type:
 * <ul>
 *   <li>redis - RedisKvStore (uses injected RedissonClient)</li>
 *   <li>jdbc - JdbcKvStore (uses JdbcTemplate)</li>
 *   <li>memory - MemoryKvStore (configurable cleanup interval)</li>
 *   <li>auto (default) - auto-detect: RedissonClient → MemoryKvStore</li>
 * </ul>
 */
@AutoConfiguration(afterName = {
    "io.mango.infra.kv.starter.redis.KvRedisAutoConfiguration",
    "io.mango.infra.persistence.starter.PersistenceAutoConfiguration",
    "org.redisson.spring.starter.RedissonAutoConfiguration",
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration"
})
@EnableConfigurationProperties(KvStoreProperties.class)
@ConditionalOnClass(IKvStore.class)
public class KvStoreAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(KvStoreAutoConfiguration.class);

    // ==================== Explicit Store Selection ====================

    /**
     * Force RedisKvStore when mango.kv.store.type=redis.
     */
    @Bean
    @ConditionalOnExpression("'${mango.kv.store.type:auto}' == 'redis'")
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore redisKvStore(RedissonClient redissonClient) {
        LOGGER.info("KV store initialized: RedisKvStore (mango.kv.store.type=redis)");
        return new RedisKvStore(redissonClient);
    }

    /**
     * Force JdbcKvStore when mango.kv.store.type=jdbc.
     */
    @Bean
    @ConditionalOnExpression("'${mango.kv.store.type:auto}' == 'jdbc'")
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore jdbcKvStore(JdbcTemplate jdbcTemplate, KvStoreProperties props) {
        String tableName = props.getProvider().getJdbc().getTableName();
        LOGGER.info("KV store initialized: JdbcKvStore (mango.kv.store.type=jdbc, tableName={})", tableName);
        return new JdbcKvStore(jdbcTemplate, tableName);
    }

    /**
     * Force MemoryKvStore when mango.kv.store.type=memory.
     */
    @Bean
    @ConditionalOnExpression("'${mango.kv.store.type:auto}' == 'memory'")
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore memoryKvStore(KvStoreProperties props) {
        int interval = props.getProvider().getMemory().getCleanupIntervalMinutes();
        LOGGER.info("KV store initialized: MemoryKvStore (mango.kv.store.type=memory, cleanupInterval={}min)", interval);
        return new MemoryKvStore(interval);
    }

    // ==================== Auto-Detection (type=auto or not configured) ====================

    /**
     * Auto-detect with RedissonClient available → use RedisKvStore
     * Only active when type=auto or not configured (matchIfMissing=true)
     */
    @Bean
    @ConditionalOnExpression("'${mango.kv.store.type:auto}' == 'auto'")
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore autoRedisKvStore(RedissonClient redissonClient) {
        LOGGER.info("KV store auto-detected: RedisKvStore (RedissonClient available)");
        return new RedisKvStore(redissonClient);
    }

    /**
     * Auto-detect fallback → use MemoryKvStore (no dependencies)
     * Only active when type=auto or not configured and no RedissonClient present
     */
    @Bean
    @ConditionalOnExpression("'${mango.kv.store.type:auto}' == 'auto'")
    @ConditionalOnMissingBean(value = RedissonClient.class, ignored = IKvStore.class)
    public IKvStore autoMemoryKvStore(KvStoreProperties props) {
        int interval = props.getProvider().getMemory().getCleanupIntervalMinutes();
        LOGGER.info("KV store auto-detected: MemoryKvStore (no RedissonClient)");
        return new MemoryKvStore(interval);
    }
}
