package io.mango.captcha.starter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.starter.config.CaptchaAutoConfiguration;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.MemoryKvStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

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
        // 使用内存实现的 IKvStore 用于测试
        return new MemoryKvStore(1);
    }
}
