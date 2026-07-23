package io.mango.infra.module.starter;

import io.mango.infra.module.api.ModuleInfo;
import io.mango.infra.module.api.ModuleInfoRegistry;
import io.mango.infra.module.api.ModuleInfoResolver;
import io.mango.infra.module.core.MemoryModuleInfoRegistry;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticContributor;
import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.infra.module.api.diagnostic.ModuleInstallationRegistry;
import io.mango.infra.module.core.diagnostic.MemoryModuleInstallationRegistry;
import io.mango.infra.module.core.diagnostic.ModuleDiagnosticAggregator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * Mango 模块信息自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(ModuleProperties.class)
@ConditionalOnProperty(prefix = "mango.module.module-service", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ModuleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ModuleMetadataLoader moduleMetadataLoader() {
        return new ModuleMetadataLoader();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mango.module.diagnostics.endpoint.enabled", havingValue = "true")
    public ModuleArtifactVersionResolver moduleArtifactVersionResolver() {
        return new ModuleArtifactVersionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mango.module.diagnostics.endpoint.enabled", havingValue = "true")
    public ModuleInstallationRegistry moduleInstallationRegistry(
            ModuleMetadataLoader metadataLoader,
            ModuleArtifactVersionResolver versionResolver) {
        MemoryModuleInstallationRegistry registry = new MemoryModuleInstallationRegistry();
        metadataLoader.load().forEach(metadata -> {
            ModuleArtifactVersionResolver.VersionResult version = versionResolver.resolve(
                    metadata.resourceUrl(), metadata.moduleName());
            registry.register(new ModuleInstallation(
                    metadata.moduleName(),
                    version.version(),
                    version.source(),
                    metadata.diagnosticAttributes()));
        });
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mango.module.diagnostics.endpoint.enabled", havingValue = "true")
    public ModuleInstallationDiagnosticContributor moduleInstallationDiagnosticContributor(
            ModuleInstallationRegistry registry) {
        return new ModuleInstallationDiagnosticContributor(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mango.module.diagnostics.endpoint.enabled", havingValue = "true")
    public ModuleDiagnosticAggregator moduleDiagnosticAggregator(List<ModuleDiagnosticContributor> contributors) {
        return new ModuleDiagnosticAggregator(contributors);
    }

    @Bean
    @ConditionalOnMissingBean
    public ModuleInfoRegistry moduleInfoRegistry(ModuleProperties properties,
                                                 ModuleMetadataLoader metadataLoader,
                                                 Environment environment) {
        MemoryModuleInfoRegistry registry = new MemoryModuleInfoRegistry();
        String defaultServiceName = environment.getProperty("spring.application.name", "application");
        String defaultContextPath = resolveContextPath(environment);

        metadataLoader.load().forEach(metadata -> resolveModulePaths(
                metadata.modulePath(),
                deriveModulePath(metadata.moduleName()))
                .forEach(modulePath -> registry.register(new ModuleInfo(
                        metadata.moduleName(),
                        defaultServiceName,
                        defaultContextPath,
                        modulePath,
                        metadata.sourceDescription()))));

        properties.getModules().forEach((moduleName, moduleService) -> {
            List<ModuleInfo> configuredModules = resolveModulePaths(
                    moduleService.getModulePath(),
                    deriveModulePath(moduleName)).stream()
                    .map(modulePath -> new ModuleInfo(
                            moduleName,
                            defaultIfBlank(moduleService.getServiceName(), defaultServiceName),
                            defaultIfBlank(moduleService.getContextPath(), defaultContextPath),
                            modulePath,
                            "config"))
                    .toList();
            registry.replace(moduleName, configuredModules);
        });

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public ModuleInfoResolver moduleInfoResolver(ModuleInfoRegistry moduleInfoRegistry) {
        return moduleInfoRegistry::resolve;
    }

    private String resolveContextPath(Environment environment) {
        String servletContextPath = environment.getProperty("server.servlet.context-path");
        if (servletContextPath != null && !servletContextPath.isBlank()) {
            return servletContextPath;
        }
        return environment.getProperty("spring.webflux.base-path", "");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private List<String> resolveModulePaths(String value, String defaultValue) {
        String source = defaultIfBlank(value, defaultValue);
        return Arrays.stream(source.split(","))
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .toList();
    }

    private String deriveModulePath(String moduleName) {
        String normalized = "";
        if (moduleName != null) {
            normalized = moduleName.trim();
        }
        if (normalized.startsWith("mango-infra-")) {
            normalized = normalized.substring("mango-infra-".length());
        } else if (normalized.startsWith("mango-")) {
            normalized = normalized.substring("mango-".length());
        }
        return "/" + normalized;
    }
}
