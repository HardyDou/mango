package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.ResourceRegistryApi;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.FileResourceProvider;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 资源声明扫描同步自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(ResourceRegistryProperties.class)
public class ResourceSyncAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResourceDeclarationLoader resourceDeclarationLoader(ObjectMapper objectMapper,
                                                               ResourceRegistryProperties properties) {
        return new ResourceDeclarationLoader(objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileResourceProvider fileResourceProvider(ResourceDeclarationLoader loader) {
        return new FileResourceProvider(loader);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceDeclarationCollector resourceDeclarationCollector(ObjectProvider<ResourceProvider> providers) {
        return new ResourceDeclarationCollector(providers);
    }

    @Bean
    @ConditionalOnBean(ResourceRegistryApi.class)
    @ConditionalOnMissingBean
    public ResourceSyncRunner resourceSyncRunner(ResourceRegistryProperties properties,
                                                 ResourceDeclarationCollector collector,
                                                 ResourceRegistryApi resourceRegistryApi,
                                                 @Value("${spring.application.name:}") String applicationName) {
        return new ResourceSyncRunner(properties, collector, resourceRegistryApi, applicationName);
    }
}
