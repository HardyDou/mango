package io.mango.infra.kv.starter.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

/**
 * Redis Auto Configuration.
 *
 * 配置优先级（从高到低）：
 *   1. mango.dal.provider.redis.* （DAL 专属配置）
 *   2. mango.redis.*              （Mango Redis 扩展配置）
 *   3. spring.redis.*             （Spring 标准配置）
 *   4. 内置默认值
 *
 * Sentinel / Cluster 等拓扑模式通过 Redisson YAML 配置实现，
 * 不在本模块范围内。
 */
@AutoConfiguration
@EnableConfigurationProperties(KvRedisProperties.class)
@ConditionalOnExpression("'${mango.kv.store.type:auto}' == 'auto' || '${mango.kv.store.type:auto}' == 'redis'")
public class KvRedisAutoConfiguration {

    @Bean
    public RedissonClient redissonClient(KvRedisProperties mango, Environment env) {
        Config config = new Config();

        String host = resolveHost(mango, env);
        int port = resolvePort(mango, env);
        String password = resolvePassword(mango, env);
        int database = resolveDatabase(mango, env);
        int timeout = resolveTimeout(mango, env);
        int maxActive = resolvePoolMaxActive(mango, env);
        int minIdle = resolvePoolMinIdle(mango, env);
        int maxIdle = resolvePoolMaxIdle(mango, env);
        int maxWait = resolvePoolMaxWait(mango, env);

        String address = String.format("redis://%s:%d", host, port);

        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setConnectTimeout(timeout)
                .setPassword(StringUtils.hasText(password) ? password : null)
                .setConnectionPoolSize(maxActive)
                .setConnectionMinimumIdleSize(minIdle)
                .setIdleConnectionTimeout(maxIdle)
                .setTimeout(timeout);

        return Redisson.create(config);
    }

    // ==================== 配置解析（优先级：mango.dal.provider.redis.* > mango.redis.* > spring.redis.* > 默认值） ====================

    private String resolveHost(KvRedisProperties mango, @NonNull Environment env) {
        // 1. mango.dal.provider.redis.host
        String dalHost = env.getProperty("mango.dal.provider.redis.host");
        if (StringUtils.hasText(dalHost)) {
            return dalHost;
        }
        // 2. mango.redis.host
        if (StringUtils.hasText(mango.getHost()) && !"localhost".equals(mango.getHost())) {
            return mango.getHost();
        }
        // 3. spring.redis.host
        return env.getProperty("spring.redis.host", "localhost");
    }

    private int resolvePort(KvRedisProperties mango, @NonNull Environment env) {
        String dalPort = env.getProperty("mango.dal.provider.redis.port");
        if (dalPort != null) {
            return Integer.parseInt(dalPort);
        }
        if (mango.getPort() != 6379) {
            return mango.getPort();
        }
        return getIntProperty(env, "spring.redis.port", 6379);
    }

    private String resolvePassword(KvRedisProperties mango, @NonNull Environment env) {
        String dalPwd = env.getProperty("mango.dal.provider.redis.password");
        if (StringUtils.hasText(dalPwd)) {
            return dalPwd;
        }
        if (StringUtils.hasText(mango.getPassword())) {
            return mango.getPassword();
        }
        return env.getProperty("spring.redis.password");
    }

    private int resolveDatabase(KvRedisProperties mango, @NonNull Environment env) {
        String dalDb = env.getProperty("mango.dal.provider.redis.database");
        if (dalDb != null) {
            return Integer.parseInt(dalDb);
        }
        if (mango.getDatabase() != 0) {
            return mango.getDatabase();
        }
        return getIntProperty(env, "spring.redis.database", 0);
    }

    private int resolveTimeout(KvRedisProperties mango, @NonNull Environment env) {
        String dalTimeout = env.getProperty("mango.dal.provider.redis.timeout");
        if (dalTimeout != null) {
            return Integer.parseInt(dalTimeout);
        }
        if (mango.getTimeout() != 3000) {
            return mango.getTimeout();
        }
        return getIntProperty(env, "spring.redis.timeout", 3000);
    }

    private int resolvePoolMaxActive(KvRedisProperties mango, @NonNull Environment env) {
        String dalMaxActive = env.getProperty("mango.dal.provider.redis.pool.maxActive");
        if (dalMaxActive != null) {
            return Integer.parseInt(dalMaxActive);
        }
        if (mango.getPool().getMaxActive() != 8) {
            return mango.getPool().getMaxActive();
        }
        return getIntProperty(env, "spring.redis.jedis.pool.max-active",
               getIntProperty(env, "spring.redis.lettuce.pool.max-active", 8));
    }

    private int resolvePoolMinIdle(KvRedisProperties mango, @NonNull Environment env) {
        String dalMinIdle = env.getProperty("mango.dal.provider.redis.pool.minIdle");
        if (dalMinIdle != null) {
            return Integer.parseInt(dalMinIdle);
        }
        if (mango.getPool().getMinIdle() != 0) {
            return mango.getPool().getMinIdle();
        }
        return getIntProperty(env, "spring.redis.jedis.pool.min-idle",
               getIntProperty(env, "spring.redis.lettuce.pool.min-idle", 0));
    }

    private int resolvePoolMaxIdle(KvRedisProperties mango, @NonNull Environment env) {
        String dalMaxIdle = env.getProperty("mango.dal.provider.redis.pool.maxIdle");
        if (dalMaxIdle != null) {
            return Integer.parseInt(dalMaxIdle);
        }
        if (mango.getPool().getMaxIdle() != 8) {
            return mango.getPool().getMaxIdle();
        }
        return getIntProperty(env, "spring.redis.jedis.pool.max-idle",
               getIntProperty(env, "spring.redis.lettuce.pool.max-idle", 8));
    }

    private int resolvePoolMaxWait(KvRedisProperties mango, @NonNull Environment env) {
        String dalMaxWait = env.getProperty("mango.dal.provider.redis.pool.maxWait");
        if (dalMaxWait != null) {
            return Integer.parseInt(dalMaxWait);
        }
        if (mango.getPool().getMaxWait() != -1) {
            return mango.getPool().getMaxWait();
        }
        return getIntProperty(env, "spring.redis.jedis.pool.max-wait",
               getIntProperty(env, "spring.redis.lettuce.pool.max-wait", -1));
    }

    private int getIntProperty(Environment env, String key, int defaultValue) {
        String value = env.getProperty(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }
}
