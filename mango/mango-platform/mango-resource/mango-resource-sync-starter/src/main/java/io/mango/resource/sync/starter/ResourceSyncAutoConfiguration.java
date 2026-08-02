package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.bootstrap.api.BootstrapRuntimeAuthorityProvider;
import io.mango.resource.sync.starter.controller.ResourceTargetController;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.FileResourceProvider;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.support.execution.DefaultResourceTargetExecutor;
import io.mango.resource.support.execution.ResourceTargetExecutor;
import io.mango.resource.support.sync.StartupReadinessStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 资源声明扫描同步自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(ResourceRegistryProperties.class)
@Import(ResourceTargetController.class)
public class ResourceSyncAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResourceTargetExecutor resourceTargetExecutor(ObjectMapper objectMapper,
                                                         ObjectProvider<ResourceHandler> handlers) {
        return new DefaultResourceTargetExecutor(objectMapper, () -> handlers.orderedStream().toList());
    }

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
    public ResourceManifestSerializer resourceManifestSerializer() {
        return new ResourceManifestSerializer();
    }

    @Bean
    @ConditionalOnBean(ResourceDeclarationApi.class)
    @ConditionalOnMissingBean
    public ResourceBootstrapStepContributor resourceBootstrapStepContributor(
            ResourceRegistryProperties properties,
            ResourceDeclarationCollector collector,
            ResourceDeclarationApi resourceDeclarationApi,
            ResourceManifestSerializer manifestSerializer,
            @Value("${spring.application.name:}") String applicationName) {
        return new ResourceBootstrapStepContributor(
                properties, collector, resourceDeclarationApi, manifestSerializer, applicationName);
    }

    @Bean
    @ConditionalOnBean({ResourceDeclarationApi.class, BootstrapRuntimeAuthorityProvider.class})
    @ConditionalOnMissingBean
    public ResourceEventualReconciliationWorker resourceEventualReconciliationWorker(
            ResourceRegistryProperties properties,
            ResourceDeclarationCollector collector,
            ResourceDeclarationApi resourceDeclarationApi,
            ResourceManifestSerializer manifestSerializer,
            BootstrapRuntimeAuthorityProvider authorityProvider,
            @Value("${spring.application.name:}") String applicationName) {
        return new ResourceEventualReconciliationWorker(properties, collector, resourceDeclarationApi,
                manifestSerializer, authorityProvider, applicationName);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceStartupReadinessCoordinator resourceStartupReadinessCoordinator(
            ObjectProvider<StartupReadinessStatus> statuses,
            ApplicationContext applicationContext,
            ObjectProvider<ApplicationAvailability> applicationAvailability) {
        return new ResourceStartupReadinessCoordinator(statuses, applicationContext,
                applicationAvailability.getIfAvailable(ApplicationAvailabilityBean::new));
    }

    /** Actuator integration remains optional for applications that do not expose management endpoints. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(HealthIndicator.class)
    static class ResourceStartupHealthConfiguration {

        @Bean("resourceStartupHealthIndicator")
        @ConditionalOnMissingBean(name = "resourceStartupHealthIndicator")
        HealthIndicator resourceStartupHealthIndicator(ObjectProvider<StartupReadinessStatus> statuses) {
            return new ResourceStartupHealthIndicator(statuses);
        }
    }
}
