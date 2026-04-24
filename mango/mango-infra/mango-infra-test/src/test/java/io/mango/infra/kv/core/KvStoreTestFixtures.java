package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IKvSortedSet;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.infra.kv.core.redis.RedisKvStore;
import org.junit.jupiter.params.provider.Arguments;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class KvStoreTestFixtures {

    private KvStoreTestFixtures() {
    }

    public static Stream<Arguments> kvStores() {
        List<Arguments> fixtures = new ArrayList<>();
        fixtures.add(Arguments.of("memory", new StoreFixture(new MemoryKvStore(), null, "kv-store-memory:")));
        fixtures.add(Arguments.of("jdbc", new StoreFixture(jdbcStore(), null, "kv-store-jdbc:")));
        redisStore().ifPresent(fixture -> fixtures.add(Arguments.of("redis", fixture)));
        return fixtures.stream();
    }

    public static Stream<Arguments> sortedSets() {
        List<Arguments> fixtures = new ArrayList<>();
        fixtures.add(Arguments.of("memory", new StoreFixture(new MemoryKvStore(), null, "sorted-set-memory:")));
        fixtures.add(Arguments.of("jdbc", new StoreFixture(jdbcStore(), null, "sorted-set-jdbc:")));
        redisStore().ifPresent(fixture -> fixtures.add(Arguments.of("redis", fixture)));
        return fixtures.stream();
    }

    private static JdbcKvStore jdbcStore() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:kv_test_" + System.nanoTime()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE infra_kv_entry (
                    id          BIGINT NOT NULL,
                    kv_key      VARCHAR(200) NOT NULL,
                    kv_value    TEXT,
                    expire_time DATETIME NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_kv_key (kv_key)
                )
                """);

        return new JdbcKvStore(jdbcTemplate);
    }

    private static Optional<StoreFixture> redisStore() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setDatabase(0)
                .setConnectTimeout(500)
                .setTimeout(500)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(2);
        RedissonClient redissonClient = null;
        try {
            redissonClient = Redisson.create(config);
            redissonClient.getKeys().count();
            String keyPrefix = "kv-test:" + System.nanoTime() + ":";
            RedissonClient client = redissonClient;
            return Optional.of(new StoreFixture(
                    new RedisKvStore(redissonClient),
                    client::shutdown,
                    keyPrefix
            ));
        } catch (Exception e) {
            if (redissonClient != null) {
                redissonClient.shutdown();
            }
            return Optional.empty();
        }
    }

    public record StoreFixture(IKvStore store, AutoCloseable closeable, String keyPrefix) implements AutoCloseable {
        public String key(String key) {
            return keyPrefix + key;
        }

        public IKvStore namespacedStore() {
            return new IKvStore() {
                @Override
                public boolean setIfAbsent(String key, String value, long expireSeconds) {
                    return store.setIfAbsent(StoreFixture.this.key(key), value, expireSeconds);
                }

                @Override
                public void set(String key, String value, long expireSeconds) {
                    store.set(StoreFixture.this.key(key), value, expireSeconds);
                }

                @Override
                public String get(String key) {
                    return store.get(StoreFixture.this.key(key));
                }

                @Override
                public long incrementBy(String key, long delta, long windowSeconds) {
                    return store.incrementBy(StoreFixture.this.key(key), delta, windowSeconds);
                }

                @Override
                public void delete(String key) {
                    store.delete(StoreFixture.this.key(key));
                }

                @Override
                public boolean exists(String key) {
                    return store.exists(StoreFixture.this.key(key));
                }
            };
        }

        public IKvStore rawStore() {
            return store;
        }

        public IKvSortedSet sortedSet() {
            return (IKvSortedSet) store;
        }

        @Override
        public void close() throws Exception {
            if (closeable != null) {
                closeable.close();
            }
            if (store instanceof AutoCloseable autoCloseable) {
                autoCloseable.close();
            }
        }
    }
}
