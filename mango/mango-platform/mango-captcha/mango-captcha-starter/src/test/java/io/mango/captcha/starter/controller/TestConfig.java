package io.mango.captcha.starter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.starter.config.CaptchaAutoConfiguration;
import io.mango.infra.kv.api.IKvStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集成测试配置
 */
@TestConfiguration
@Import(CaptchaAutoConfiguration.class)
public class TestConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public IKvStore ikvStore() {
        return new TestKvStore();
    }

    private static final class TestKvStore implements IKvStore {

        private final Map<String, Entry> values = new ConcurrentHashMap<>();

        @Override
        public boolean setIfAbsent(String key, String value, long expireSeconds) {
            validateKey(key);
            if (expireSeconds <= 0) {
                delete(key);
                return false;
            }
            Entry existing = values.get(key);
            if (existing != null && !existing.expired()) {
                return false;
            }
            values.put(key, new Entry(value, Instant.now().plusSeconds(expireSeconds)));
            return true;
        }

        @Override
        public void set(String key, String value, long expireSeconds) {
            validateKey(key);
            if (expireSeconds <= 0) {
                delete(key);
                return;
            }
            values.put(key, new Entry(value, Instant.now().plusSeconds(expireSeconds)));
        }

        @Override
        public boolean put(String key, String value, long expireSeconds) {
            return setIfAbsent(key, value, expireSeconds);
        }

        @Override
        public String get(String key) {
            validateKey(key);
            Entry entry = values.get(key);
            if (entry == null || entry.expired()) {
                values.remove(key);
                return null;
            }
            return entry.value();
        }

        @Override
        public long incrementBy(String key, long delta, long windowSeconds) {
            validateKey(key);
            Entry entry = values.compute(key, (ignored, current) -> {
                long next = current == null || current.expired() ? delta : Long.parseLong(current.value()) + delta;
                return new Entry(String.valueOf(next), Instant.now().plusSeconds(windowSeconds));
            });
            return Long.parseLong(entry.value());
        }

        @Override
        public long increment(String key, long windowSeconds) {
            return incrementBy(key, 1, windowSeconds);
        }

        @Override
        public void delete(String key) {
            validateKey(key);
            values.remove(key);
        }

        @Override
        public boolean exists(String key) {
            return get(key) != null;
        }

        private void validateKey(String key) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key cannot be null or blank");
            }
        }
    }

    private record Entry(String value, Instant expireAt) {

        private boolean expired() {
            return expireAt.isBefore(Instant.now());
        }
    }
}
