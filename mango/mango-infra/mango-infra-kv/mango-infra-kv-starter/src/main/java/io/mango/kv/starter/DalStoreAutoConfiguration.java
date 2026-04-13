package io.mango.kv.starter;

import io.mango.kv.api.IKvStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import io.mango.kv.core.JdbcKvStore;
import io.mango.kv.core.MemoryKvStore;
import io.mango.kv.core.RedisKvStore;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * DAL store auto-configuration.
 *
 * Creates IKvStore bean based on mango.dal.type property:
 * <ul>
 *   <li>redis - RedisKvStore (uses injected RedissonClient)</li>
 *   <li>db - JdbcKvStore (uses JdbcTemplate + injected RedissonClient)</li>
 *   <li>memory - MemoryKvStore (可配置清理间隔)</li>
 *   <li>auto (default) - auto-detect: RedissonClient → MemoryKvStore</li>
 * </ul>
 *
 * 配置结构：
 * <pre>
 * mango:
 *   dal:
 *     type: auto/redis/db/memory
 *     provider:
 *       redis:
 *         host: localhost
 *         port: 6379
 *         password:
 *         database: 0
 *         timeout: 3000
 *         pool:
 *           maxActive: 8
 *           maxIdle: 8
 *           minIdle: 0
 *           maxWait: -1
 *       db:
 *         tableName: sys_kv_record
 *       memory:
 *         cleanupIntervalMinutes: 1
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(DalStoreProperties.class)
@ConditionalOnClass(IKvStore.class)
public class DalStoreAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DalStoreAutoConfiguration.class);

    // ==================== Explicit Type Selection ====================

    /**
     * Force RedisKvStore when mango.dal.type=redis
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal", name = "type", havingValue = "redis")
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore redisKvStore(RedissonClient redissonClient) {
        log.info("DAL Store initialized: RedisKvStore (mango.dal.type=redis)");
        return new RedisKvStore(redissonClient);
    }

    /**
     * Force JdbcKvStore when mango.dal.type=db
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal", name = "type", havingValue = "db")
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore dbKvStore(JdbcTemplate jdbcTemplate, RedissonClient redissonClient) {
        log.info("DAL Store initialized: JdbcKvStore (mango.dal.type=db)");
        return new JdbcKvStore(jdbcTemplate, redissonClient);
    }

    /**
     * Force MemoryKvStore when mango.dal.type=memory
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal", name = "type", havingValue = "memory")
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore memoryKvStore(DalStoreProperties props) {
        int interval = props.getProvider().getMemory().getCleanupIntervalMinutes();
        log.info("DAL Store initialized: MemoryKvStore (mango.dal.type=memory, cleanupInterval={}min)", interval);
        return new MemoryKvStore(interval);
    }

    // ==================== Auto-Detection (type=auto or not configured) ====================

    /**
     * Auto-detect with RedissonClient available → use RedisKvStore
     * Only active when type=auto or not configured (matchIfMissing=true)
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal", name = "type", havingValue = "auto", matchIfMissing = true)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore autoRedisKvStore(RedissonClient redissonClient) {
        log.info("DAL Store auto-detected: RedisKvStore (RedissonClient available)");
        return new RedisKvStore(redissonClient);
    }

    /**
     * Auto-detect fallback → use MemoryKvStore (no dependencies)
     * Only active when type=auto or not configured and no RedissonClient present
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal", name = "type", havingValue = "auto", matchIfMissing = true)
    @ConditionalOnMissingBean(value = RedissonClient.class, ignored = IKvStore.class)
    public IKvStore autoMemoryKvStore(DalStoreProperties props) {
        int interval = props.getProvider().getMemory().getCleanupIntervalMinutes();
        log.info("DAL Store auto-detected: MemoryKvStore (no RedissonClient)");
        return new MemoryKvStore(interval);
    }
}
