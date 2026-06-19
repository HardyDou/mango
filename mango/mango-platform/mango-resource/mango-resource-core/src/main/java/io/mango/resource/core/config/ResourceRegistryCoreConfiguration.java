package io.mango.resource.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.kv.api.ILocker;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.core.mapper.ResourceChangeLogMapper;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.mapper.ResourceSyncLogMapper;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceRegistryLock;
import io.mango.resource.core.sync.ResourceRegistryRepository;
import io.mango.resource.core.sync.ResourceRegistrySyncService;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.FileResourceProvider;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 资源注册中心核心装配。
 */
@Configuration
@EnableConfigurationProperties(ResourceRegistryProperties.class)
public class ResourceRegistryCoreConfiguration {

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
    @ConditionalOnMissingBean
    public ResourceContentHasher resourceContentHasher(ObjectMapper objectMapper) {
        return new ResourceContentHasher(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceRegistryRepository resourceRegistryRepository(ResourceRegistryMapper registryMapper,
                                                                 ResourceSyncLogMapper syncLogMapper,
                                                                 ResourceChangeLogMapper changeLogMapper) {
        return new ResourceRegistryRepository(registryMapper, syncLogMapper, changeLogMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceRegistryLock resourceRegistryLock(ILocker locker) {
        return new ResourceRegistryLock(locker);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceRegistrySyncService resourceRegistrySyncService(ResourceRegistryProperties properties,
                                                                   ResourceDeclarationCollector collector,
                                                                   ObjectProvider<ResourceHandler> handlers,
                                                                   ResourceContentHasher hasher,
                                                                   ResourceRegistryRepository repository,
                                                                   ResourceRegistryLock lock,
                                                                   ObjectMapper objectMapper) {
        return new ResourceRegistrySyncService(properties, collector, handlers, hasher, repository, lock, objectMapper);
    }

}
