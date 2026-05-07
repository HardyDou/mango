package io.mango.infra.module.starter;

import io.mango.infra.module.api.ModuleInfo;
import io.mango.infra.module.api.ModuleInfoRegistry;
import io.mango.infra.module.api.ModuleInfoResolver;
import io.mango.infra.module.core.MemoryModuleInfoRegistry;
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
                        metadata.source()))));

        properties.getModules().forEach((moduleName, moduleService) -> resolveModulePaths(
                moduleService.getModulePath(),
                deriveModulePath(moduleName))
                .forEach(modulePath -> registry.register(new ModuleInfo(
                        moduleName,
                        defaultIfBlank(moduleService.getServiceName(), defaultServiceName),
                        defaultIfBlank(moduleService.getContextPath(), defaultContextPath),
                        modulePath,
                        "config"))));

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
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private List<String> resolveModulePaths(String value, String defaultValue) {
        String source = defaultIfBlank(value, defaultValue);
        return Arrays.stream(source.split(","))
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .toList();
    }

    private String deriveModulePath(String moduleName) {
        String normalized = moduleName == null ? "" : moduleName.trim();
        if (normalized.startsWith("mango-infra-")) {
            normalized = normalized.substring("mango-infra-".length());
        } else if (normalized.startsWith("mango-")) {
            normalized = normalized.substring("mango-".length());
        }
        return "/" + normalized;
    }
}
