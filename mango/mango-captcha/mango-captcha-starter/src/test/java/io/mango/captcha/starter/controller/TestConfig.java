package io.mango.captcha.starter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.starter.config.CaptchaAutoConfiguration;
import io.mango.kv.api.IKvStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
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
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public IKvStore ikvStore() {
        // 使用内存实现的 IKvStore 用于测试
        return new IKvStore() {
            private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

            @Override
            public boolean put(String key, String value, long expireSeconds) {
                map.put(key, value);
                return true;
            }

            @Override
            public String get(String key) {
                return map.get(key);
            }

            @Override
            public long increment(String key, long windowSeconds) {
                map.put(key, String.valueOf(map.size() + 1));
                return map.size();
            }

            @Override
            public void delete(String key) {
                map.remove(key);
            }

            @Override
            public boolean exists(String key) {
                return map.containsKey(key);
            }
        };
    }
}
