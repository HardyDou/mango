package io.mango.infra.persistence.starter;

import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.persistence.starter.scope.MybatisPlusDataScopeApplier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * 持久化基础设施自动配置。
 * <p>
 * 统一承载关系型数据库相关基础能力，包括数据源、事务、数据库迁移、
 * MyBatis-Plus 插件和 Repository 契约等。业务模块只依赖本 starter，
 * 不直接感知底层持久化组件组合。
 */
@AutoConfiguration
@EnableTransactionManagement
@EnableConfigurationProperties(PersistenceProperties.class)
public class PersistenceAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataScopeProvider.class)
    @ConditionalOnMissingBean(DataScopeApplier.class)
    public DataScopeApplier dataScopeApplier(DataScopeProvider dataScopeProvider,
                                             ObjectProvider<DataSource> dataSourceProvider) {
        return new MybatisPlusDataScopeApplier(dataScopeProvider,
                Optional.ofNullable(dataSourceProvider.getIfAvailable()));
    }
}
