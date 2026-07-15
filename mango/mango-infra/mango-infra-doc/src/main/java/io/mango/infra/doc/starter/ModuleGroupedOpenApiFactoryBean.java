package io.mango.infra.doc.starter;

import io.mango.infra.module.api.ModuleInfo;
import io.mango.infra.module.api.ModuleInfoRegistry;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 在 OpenAPI 分组实例化时读取当前运行时模块路径。
 */
public class ModuleGroupedOpenApiFactoryBean implements FactoryBean<GroupedOpenApi>, BeanFactoryAware {

    private final String moduleName;
    private final List<String> fallbackPaths;
    private BeanFactory beanFactory;

    public ModuleGroupedOpenApiFactoryBean(String moduleName, List<String> fallbackPaths) {
        this.moduleName = moduleName;
        this.fallbackPaths = List.copyOf(fallbackPaths);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public GroupedOpenApi getObject() {
        DocProperties properties = beanFactory.getBean(DocProperties.class);
        List<String> modulePaths = resolveModulePaths();
        String[] pathPatterns = pathsToMatch(modulePaths);
        return GroupedOpenApi.builder()
                .group(moduleName)
                .displayName(moduleName + " (" + String.join(", ", modulePaths) + ")")
                .pathsToMatch(pathPatterns)
                .addOperationCustomizer(new MangoApiScopeOperationCustomizer(
                        properties.getModuleGrouping().isIncludeScopeTags()))
                .build();
    }

    @Override
    public Class<?> getObjectType() {
        return GroupedOpenApi.class;
    }

    private List<String> resolveModulePaths() {
        ModuleInfoRegistry registry = beanFactory.getBeanProvider(ModuleInfoRegistry.class).getIfAvailable();
        if (registry != null) {
            List<String> configuredPaths = registry.list().stream()
                    .filter(moduleInfo -> moduleName.equals(moduleInfo.moduleName()))
                    .map(ModuleInfo::modulePath)
                    .distinct()
                    .toList();
            if (!configuredPaths.isEmpty()) {
                return configuredPaths;
            }
        }

        Set<String> normalizedPaths = new LinkedHashSet<>();
        fallbackPaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(this::normalizePath)
                .forEach(normalizedPaths::add);
        return List.copyOf(normalizedPaths);
    }

    private String normalizePath(String path) {
        return new ModuleInfo(moduleName, "openapi", "", path, "classpath").modulePath();
    }

    private String[] pathsToMatch(List<String> modulePaths) {
        List<String> patterns = new ArrayList<>();
        for (String modulePath : modulePaths) {
            if ("/".equals(modulePath)) {
                patterns.add("/**");
            } else {
                patterns.add(modulePath);
                patterns.add(modulePath + "/**");
            }
        }
        return patterns.toArray(String[]::new);
    }
}
