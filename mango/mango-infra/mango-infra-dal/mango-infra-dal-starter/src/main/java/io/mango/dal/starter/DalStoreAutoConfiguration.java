package io.mango.dal.starter;

import io.mango.dal.api.IKvStore;
import io.mango.dal.core.DbXivStore;
import io.mango.dal.core.MemoryXivStore;
import io.mango.dal.core.RedisXivStore;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * DAL store auto-configuration.
 * <p>
 * Creates IKvStore bean based on mango.dal.kvstore.type property:
 * <ul>
 *   <li>redis - force RedisXivStore (requires RedissonClient)</li>
 *   <li>db - force DbXivStore (requires DataSource)</li>
 *   <li>memory - force MemoryXivStore (no dependencies)</li>
 *   <li>auto (default) - auto-detect: RedissonClient → DataSource → MemoryXivStore</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(DalStoreProperties.class)
@ConditionalOnClass(IKvStore.class)
public class DalStoreAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DalStoreAutoConfiguration.class);

    /**
     * Properties for DAL store configuration.
     */
    @Bean
    @ConditionalOnMissingBean
    public DalStoreProperties dalStoreProperties() {
        return new DalStoreProperties();
    }

    // ==================== Explicit Type Selection ====================

    /**
     * Force RedisXivStore when mango.dal.kvstore.type=redis
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal.kvstore", name = "type", havingValue = "redis")
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore redisXivStore(RedissonClient redissonClient) {
        log.info("DAL Store initialized: RedisXivStore (forced by mango.dal.kvstore.type=redis)");
        return new RedisXivStore(redissonClient);
    }

    /**
     * Force DbXivStore when mango.dal.kvstore.type=db
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal.kvstore", name = "type", havingValue = "db")
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore dbXivStore(JdbcTemplate jdbcTemplate, RedissonClient redissonClient) {
        log.info("DAL Store initialized: DbXivStore (forced by mango.dal.kvstore.type=db)");
        return new DbXivStore(jdbcTemplate, redissonClient);
    }

    /**
     * Force MemoryXivStore when mango.dal.kvstore.type=memory
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal.kvstore", name = "type", havingValue = "memory")
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore memoryXivStore() {
        log.info("DAL Store initialized: MemoryXivStore (forced by mango.dal.kvstore.type=memory)");
        return new MemoryXivStore();
    }

    // ==================== Auto-Detection (type=auto or not configured) ====================

    /**
     * Auto-detect with RedissonClient available → use RedisXivStore
     * Only active when type=auto or not configured (matchIfMissing=true)
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal.kvstore", name = "type", havingValue = "auto", matchIfMissing = true)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(IKvStore.class)
    public IKvStore autoRedisXivStore(RedissonClient redissonClient) {
        log.info("DAL Store auto-detected: RedisXivStore (RedissonClient available)");
        return new RedisXivStore(redissonClient);
    }

    /**
     * Auto-detect fallback → use MemoryXivStore (no dependencies)
     * Only active when type=auto or not configured and no RedissonClient present
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.dal.kvstore", name = "type", havingValue = "auto", matchIfMissing = true)
    @ConditionalOnMissingBean(value = RedissonClient.class, ignored = IKvStore.class)
    public IKvStore autoMemoryXivStore() {
        log.info("DAL Store auto-detected: MemoryXivStore (no RedissonClient)");
        return new MemoryXivStore();
    }
}
