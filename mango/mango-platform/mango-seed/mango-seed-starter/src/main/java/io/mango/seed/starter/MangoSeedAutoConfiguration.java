package io.mango.seed.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration",
        "io.mango.infra.persistence.starter.PersistenceFlywayAutoConfiguration"
})
@EnableConfigurationProperties(MangoSeedProperties.class)
public class MangoSeedAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnProperty(prefix = "mango.seed", name = "enabled", havingValue = "true")
    public MangoSeedRunner mangoSeedRunner(JdbcTemplate jdbcTemplate, MangoSeedProperties properties, Environment environment) {
        return new MangoSeedRunner(jdbcTemplate, properties, passwordEncoder(), environment);
    }

    private PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
