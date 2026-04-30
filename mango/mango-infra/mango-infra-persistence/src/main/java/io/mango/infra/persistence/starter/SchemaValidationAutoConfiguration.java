package io.mango.infra.persistence.starter;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * 数据库结构校验自动配置。
 */
@AutoConfiguration(after = PersistenceAutoConfiguration.class)
@EnableConfigurationProperties(PersistenceProperties.class)
public class SchemaValidationAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnProperty(prefix = "mango.persistence.schema-validation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public ApplicationRunner persistenceSchemaValidationRunner(DataSource dataSource,
                                                               PersistenceProperties properties) {
        return args -> new SchemaValidationRunner(dataSource, properties.getSchemaValidation()).run();
    }
}
