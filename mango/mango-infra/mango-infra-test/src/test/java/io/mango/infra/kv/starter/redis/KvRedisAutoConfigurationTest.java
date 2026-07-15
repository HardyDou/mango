package io.mango.infra.kv.starter.redis;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class KvRedisAutoConfigurationTest {

    @Test
    void dalPropertiesOverrideMangoAndSpringProperties() {
        KvRedisProperties properties = configuredMangoProperties();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.redis.host", "spring-host")
                .withProperty("spring.redis.port", "6381")
                .withProperty("mango.dal.provider.redis.host", "dal-host")
                .withProperty("mango.dal.provider.redis.port", "6382")
                .withProperty("mango.dal.provider.redis.database", "4")
                .withProperty("mango.dal.provider.redis.timeout", "4200")
                .withProperty("mango.dal.provider.redis.pool.maxActive", "12")
                .withProperty("mango.dal.provider.redis.pool.minIdle", "3");

        SingleServerConfig singleServer = singleServerConfig(properties, environment);

        assertThat(singleServer.getAddress()).isEqualTo("redis://dal-host:6382");
        assertThat(singleServer.getDatabase()).isEqualTo(4);
        assertThat(singleServer.getTimeout()).isEqualTo(4200);
        assertThat(singleServer.getConnectionPoolSize()).isEqualTo(12);
        assertThat(singleServer.getConnectionMinimumIdleSize()).isEqualTo(3);
    }

    @Test
    void maxIdleConnectionCountIsNotMisusedAsIdleTimeoutMilliseconds() {
        KvRedisProperties properties = new KvRedisProperties();
        properties.getPool().setMaxIdle(2);

        SingleServerConfig singleServer = singleServerConfig(properties, new MockEnvironment());

        assertThat(singleServer.getIdleConnectionTimeout()).isGreaterThan(2);
        assertThat(singleServer.getConnectionPoolSize()).isEqualTo(KvRedisProperties.DEFAULT_MAX_ACTIVE);
    }

    private KvRedisProperties configuredMangoProperties() {
        KvRedisProperties properties = new KvRedisProperties();
        properties.setHost("mango-host");
        properties.setPort(6380);
        properties.setDatabase(2);
        properties.setTimeout(3500);
        properties.getPool().setMaxActive(10);
        properties.getPool().setMinIdle(2);
        return properties;
    }

    private SingleServerConfig singleServerConfig(KvRedisProperties properties, MockEnvironment environment) {
        Config config = new KvRedisAutoConfiguration().redissonConfig(properties, environment);
        return config.useSingleServer();
    }
}
