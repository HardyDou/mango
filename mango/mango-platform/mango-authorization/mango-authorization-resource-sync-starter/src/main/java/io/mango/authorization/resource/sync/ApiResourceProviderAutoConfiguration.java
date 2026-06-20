package io.mango.authorization.resource.sync;

import io.mango.infra.module.api.ModuleInfoRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * API 访问资源 Provider 自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(name = {
        "io.mango.resource.api.ResourceProvider",
        "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping"
})
@ConditionalOnProperty(name = "mango.authorization.resource-sync.resource-provider.enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ApiResourceSyncProperties.class)
public class ApiResourceProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApiResourceDeclarationConverter apiResourceDeclarationConverter() {
        return new ApiResourceDeclarationConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAccessResourceDiscoverer apiAccessResourceDiscoverer(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            ObjectProvider<ModuleInfoRegistry> moduleInfoRegistryProvider,
            ApiResourceSyncProperties properties) {
        return new ApiAccessResourceDiscoverer(handlerMapping, moduleInfoRegistryProvider, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAccessResourceProvider apiAccessResourceProvider(ApiAccessResourceDiscoverer discoverer,
                                                              ApiResourceSyncProperties properties,
                                                              ApiResourceDeclarationConverter converter) {
        return new ApiAccessResourceProvider(discoverer, properties, converter);
    }
}
