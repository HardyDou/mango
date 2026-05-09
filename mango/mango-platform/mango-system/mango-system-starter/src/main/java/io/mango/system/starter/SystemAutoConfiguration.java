package io.mango.system.starter;

import io.mango.infra.iplocation.api.IpLocationResolver;
import io.mango.system.core.aspect.OperationLogAspect;
import io.mango.system.core.middleware.TenantFilter;
import io.mango.system.core.service.ISysLogService;
import org.springframework.beans.factory.ObjectProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "io.mango.system.core.mapper",
        "io.mango.area.core.mapper",
        "io.mango.i18n.core.mapper"
})
@ComponentScan({
        "io.mango.system.core",
        "io.mango.system.starter.controller",
        "io.mango.area.core",
        "io.mango.i18n.core",
        "io.mango.i18n.starter.controller"
})
public class SystemAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "mango.tenant", name = "enabled", havingValue = "true")
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.log.operation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OperationLogAspect operationLogAspect(ISysLogService logService,
                                                 ObjectProvider<IpLocationResolver> ipLocationResolverProvider) {
        return new OperationLogAspect(logService, ipLocationResolverProvider.getIfAvailable());
    }
}
