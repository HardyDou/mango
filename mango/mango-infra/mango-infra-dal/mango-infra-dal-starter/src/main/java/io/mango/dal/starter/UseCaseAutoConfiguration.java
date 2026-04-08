package io.mango.dal.starter;

import io.mango.dal.api.*;
import io.mango.dal.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * IUseCase auto-configuration.
 *
 * Provides Memory implementations by default (no dependencies).
 * Redis implementations available when RedissonClient is present.
 *
 * Configuration:
 * <pre>
 * mango:
 *   dal:
 *     type: auto/memory/redis
 *     memory:
 *       cleanupIntervalMinutes: 1
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(DalStoreProperties.class)
@ConditionalOnClass({ICache.class, ILocker.class})
public class UseCaseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(UseCaseAutoConfiguration.class);

    // ==================== ICache ====================

    @Bean
    @ConditionalOnMissingBean(ICache.class)
    public ICache memoryCache(DalStoreProperties props) {
        log.info("ICache initialized: MemoryCache (cleanupInterval={}min)",
                props.getProvider().getMemory().getCleanupIntervalMinutes());
        return new MemoryCache(props.getProvider().getMemory().getCleanupIntervalMinutes());
    }

    // ==================== ILocker ====================

    @Bean
    @ConditionalOnMissingBean(ILocker.class)
    public ILocker memoryLocker() {
        log.info("ILocker initialized: MemoryLocker");
        return new MemoryLocker();
    }

    // ==================== ICounter ====================

    @Bean
    @ConditionalOnMissingBean(ICounter.class)
    public ICounter memoryCounter() {
        log.info("ICounter initialized: MemoryCounter");
        return new MemoryCounter();
    }

    // ==================== IRateLimiter ====================

    @Bean
    @ConditionalOnMissingBean(IRateLimiter.class)
    public IRateLimiter memoryRateLimiter() {
        log.info("IRateLimiter initialized: MemoryRateLimiter");
        return new MemoryRateLimiter();
    }

    // ==================== IIdempotent ====================

    @Bean
    @ConditionalOnMissingBean(IIdempotent.class)
    public IIdempotent memoryIdempotent() {
        log.info("IIdempotent initialized: MemoryIdempotent");
        return new MemoryIdempotent();
    }

    // ==================== ITokenStore ====================

    @Bean
    @ConditionalOnMissingBean(ITokenStore.class)
    public ITokenStore memoryTokenStore(DalStoreProperties props) {
        log.info("ITokenStore initialized: MemoryTokenStore (cleanupInterval={}min)",
                props.getProvider().getMemory().getCleanupIntervalMinutes());
        return new MemoryTokenStore(props.getProvider().getMemory().getCleanupIntervalMinutes());
    }

    // ==================== IIdGenerator ====================

    @Bean
    @ConditionalOnMissingBean(IIdGenerator.class)
    public IIdGenerator memoryIdGenerator() {
        log.info("IIdGenerator initialized: MemoryIdGenerator");
        return new MemoryIdGenerator();
    }

    // ==================== ISerializer ====================

    @Bean
    @ConditionalOnMissingBean(ISerializer.class)
    public ISerializer jsonSerializer() {
        log.info("ISerializer initialized: JsonSerializer");
        return new JsonSerializer();
    }

    // ==================== IConverter ====================

    @Bean
    @ConditionalOnMissingBean(IConverter.class)
    public IConverter jsonConverter() {
        log.info("IConverter initialized: JsonConverter");
        return new JsonConverter();
    }
}