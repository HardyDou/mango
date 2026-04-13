package io.mango.system.starter;

import io.mango.system.core.aspect.OperationLogAspect;
import io.mango.system.core.middleware.TenantFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "mango.tenant", name = "enabled", havingValue = "true")
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.log", name = "operation-enabled", havingValue = "true")
    public OperationLogAspect operationLogAspect() {
        return new OperationLogAspect();
    }
}
