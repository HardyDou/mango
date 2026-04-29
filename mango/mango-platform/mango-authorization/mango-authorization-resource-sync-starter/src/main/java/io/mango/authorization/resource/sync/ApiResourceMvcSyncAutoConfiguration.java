package io.mango.authorization.resource.sync;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.infra.module.api.ModuleInfoRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Spring MVC 接口资源扫描自动配置。
 *
 * @author hardy
 */
@AutoConfiguration
@AutoConfigureAfter(ApiResourceSyncAutoConfiguration.class)
@ConditionalOnProperty(name = "mango.authorization.resource-sync.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping")
public class ApiResourceMvcSyncAutoConfiguration {

    @Bean
    @ConditionalOnBean({ApiResourceApi.class, RequestMappingHandlerMapping.class})
    public ApiResourceSyncRunner apiResourceSyncRunner(
            RequestMappingHandlerMapping handlerMapping,
            ApiResourceApi apiResourceApi,
            ObjectProvider<ModuleInfoRegistry> moduleInfoRegistryProvider) {
        return new ApiResourceSyncRunner(handlerMapping, apiResourceApi, moduleInfoRegistryProvider);
    }
}
