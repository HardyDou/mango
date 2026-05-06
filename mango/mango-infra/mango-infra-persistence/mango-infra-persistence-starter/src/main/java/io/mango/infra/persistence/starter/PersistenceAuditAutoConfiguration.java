package io.mango.infra.persistence.starter;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.mango.infra.persistence.api.context.PersistenceContextProvider;
import io.mango.infra.persistence.api.scope.TenantProvider;
import io.mango.infra.persistence.starter.audit.PersistenceAuditMetaObjectHandler;
import io.mango.infra.persistence.starter.context.MangoContextPersistenceContextProvider;
import io.mango.infra.persistence.starter.context.MangoContextTenantProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 持久化审计自动配置。
 */
@AutoConfiguration(after = PersistenceAutoConfiguration.class)
@ConditionalOnClass(MetaObjectHandler.class)
@EnableConfigurationProperties(PersistenceProperties.class)
public class PersistenceAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PersistenceContextProvider.class)
    public PersistenceContextProvider persistenceContextProvider() {
        return new MangoContextPersistenceContextProvider();
    }

    @Bean
    @ConditionalOnMissingBean(TenantProvider.class)
    public TenantProvider tenantProvider(PersistenceContextProvider contextProvider) {
        return new MangoContextTenantProvider(contextProvider);
    }

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    @ConditionalOnProperty(prefix = "mango.persistence.audit",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public MetaObjectHandler persistenceAuditMetaObjectHandler(PersistenceContextProvider contextProvider) {
        return new PersistenceAuditMetaObjectHandler(contextProvider);
    }
}
