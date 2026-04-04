package io.mango.infra.redis.starter;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Redis Auto Configuration
 *
 * @author Mango
 */
@AutoConfiguration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {

    public RedissonClient redissonClient(RedisProperties properties) {
        Config config = new Config();
        String address = String.format("redis://%s:%d", properties.getHost(), properties.getPort());

        config.useSingleServer()
                .setAddress(address)
                .setDatabase(properties.getDatabase())
                .setConnectTimeout(properties.getTimeout())
                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPool().getMaxActive())
                .setConnectionMinimumIdleSize(properties.getPool().getMinIdle())
                .setIdleConnectionTimeout(properties.getPool().getMaxIdle())
                .setTimeout(properties.getTimeout());

        return Redisson.create(config);
    }
}
