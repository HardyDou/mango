package io.mango.infra.persistence.starter.datasource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mango 多数据源自动配置。
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class,
        beforeName = "com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure")
@Conditional(PersistenceManagedDataSourceCondition.class)
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(PersistenceDataSourceProperties.class)
public class PersistenceDataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PersistenceDataSourceRegistry.class)
    public PersistenceDataSourceRegistry persistenceDataSourceRegistry(PersistenceDataSourceProperties properties) {
        Map<String, DataSource> dataSources = new LinkedHashMap<>();
        properties.getDatasources().forEach((name, config) -> {
            if (config != null && StringUtils.hasText(config.getUrl())) {
                dataSources.put(name, buildDataSource(config));
            }
        });
        return new DefaultPersistenceDataSourceRegistry(properties.primaryName(), dataSources);
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(PersistenceDataSourceRegistry registry) {
        return new MangoRoutingDataSource(registry);
    }

    @Bean
    @ConditionalOnMissingBean(PersistenceModuleDataSourceDefaults.class)
    public PersistenceModuleDataSourceDefaults persistenceModuleDataSourceDefaults() {
        return new PersistenceModuleDataSourceDefaults();
    }

    @Bean
    @ConditionalOnMissingBean(PersistenceModuleDataSourceResolver.class)
    public PersistenceModuleDataSourceResolver persistenceModuleDataSourceResolver(
            PersistenceDataSourceProperties properties,
            PersistenceModuleDataSourceDefaults moduleDefaults,
            PersistenceDataSourceRegistry registry) {
        return new DefaultPersistenceModuleDataSourceResolver(properties, moduleDefaults, registry);
    }

    @Bean
    @ConditionalOnMissingBean(PersistenceDataSourceAspect.class)
    @ConditionalOnProperty(prefix = "mango.persistence.datasource-routing",
            name = "annotation-enabled",
            havingValue = "true",
            matchIfMissing = true)
    public PersistenceDataSourceAspect persistenceDataSourceAspect() {
        return new PersistenceDataSourceAspect();
    }

    private DataSource buildDataSource(PersistenceDataSourceProperties.DataSourceConfig config) {
        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .url(config.getUrl())
                .username(config.getUsername())
                .password(config.getPassword());
        if (StringUtils.hasText(config.getDriverClassName())) {
            builder.driverClassName(config.getDriverClassName());
        }
        return builder.build();
    }
}
