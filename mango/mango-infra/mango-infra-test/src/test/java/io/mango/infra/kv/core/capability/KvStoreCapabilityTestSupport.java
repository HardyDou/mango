package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.infra.kv.core.redis.RedisKvStore;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

abstract class KvStoreCapabilityTestSupport {

    static Stream<Arguments> kvStores() {
        List<Arguments> fixtures = new ArrayList<>();
        fixtures.add(Arguments.of("memory", new StoreFixture(new MemoryKvStore(), null, null, new AtomicBoolean(false))));
        fixtures.add(Arguments.of("jdbc", jdbcFixture()));
        redisFixture().ifPresent(redis -> fixtures.add(Arguments.of("redis", redis)));
        return fixtures.stream();
    }

    private static StoreFixture jdbcFixture() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:kv_capability_" + System.nanoTime()
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

        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        RAtomicLong atomicLong = Mockito.mock(RAtomicLong.class);
        Mockito.when(redissonClient.getAtomicLong(Mockito.anyString())).thenReturn(atomicLong);
        Mockito.when(atomicLong.incrementAndGet()).thenAnswer(invocation -> IdSequence.next());

        return new StoreFixture(new JdbcKvStore(jdbcTemplate, redissonClient), null, null, new AtomicBoolean(false));
    }

    private static java.util.Optional<StoreFixture> redisFixture() {
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
            String keyPrefix = "kv-capability-test:" + System.nanoTime() + ":";
            RedissonClient client = redissonClient;
            return java.util.Optional.of(new StoreFixture(
                    new RedisKvStore(redissonClient),
                    () -> {
                        client.getKeys().deleteByPattern(keyPrefix + "*");
                        client.shutdown();
                    },
                    keyPrefix,
                    new AtomicBoolean(false)
            ));
        } catch (Exception e) {
            if (redissonClient != null) {
                redissonClient.shutdown();
            }
            return java.util.Optional.empty();
        }
    }

    record StoreFixture(IKvStore store, AutoCloseable closeable, String keyPrefix, AtomicBoolean closed)
            implements AutoCloseable {
        String key(String key) {
            return keyPrefix == null ? key : keyPrefix + key;
        }

        IKvStore namespacedStore() {
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
                public boolean put(String key, String value, long expireSeconds) {
                    return store.put(StoreFixture.this.key(key), value, expireSeconds);
                }

                @Override
                public String get(String key) {
                    return store.get(StoreFixture.this.key(key));
                }

                @Override
                public long increment(String key, long windowSeconds) {
                    return store.increment(StoreFixture.this.key(key), windowSeconds);
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

        @Override
        public void close() throws Exception {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            if (closeable != null) {
                closeable.close();
            }
            if (store instanceof AutoCloseable autoCloseable) {
                autoCloseable.close();
            }
        }
    }

    private static final class IdSequence {
        private static long current = 0;

        private static synchronized long next() {
            return ++current;
        }
    }
}
