package io.mango.resource.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.core.mapper.ResourceChangeLogMapper;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.mapper.ResourceSyncLogMapper;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceRegistryLock;
import io.mango.resource.core.sync.ResourceRegistryRepository;
import io.mango.resource.core.diagnostic.ResourceModuleDiagnosticContributor;
import io.mango.resource.core.diagnostic.ResourceModuleSyncStatusRegistry;
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
    public ResourceRegistryLock resourceRegistryLock(ILeaseLocker locker) {
        return new ResourceRegistryLock(locker);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceModuleSyncStatusRegistry resourceModuleSyncStatusRegistry(ResourceContentHasher hasher) {
        return new ResourceModuleSyncStatusRegistry(hasher);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceModuleDiagnosticContributor resourceModuleDiagnosticContributor(
            ResourceModuleSyncStatusRegistry registry,
            ResourceRegistryProperties properties) {
        return new ResourceModuleDiagnosticContributor(registry, properties);
    }

}
