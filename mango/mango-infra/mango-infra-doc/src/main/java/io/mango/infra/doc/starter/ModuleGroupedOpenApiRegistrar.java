package io.mango.infra.doc.starter;

import io.mango.infra.module.starter.ModuleMetadataLoader;
import io.mango.infra.module.starter.ModuleProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据 Mango 模块元数据动态注册 OpenAPI 分组。
 */
public class ModuleGroupedOpenApiRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final String BEAN_NAME_PREFIX = "mangoDocGroupedOpenApi_";
    private final ModuleMetadataLoader metadataLoader = new ModuleMetadataLoader();
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry)
            throws BeansException {
        DocProperties docProperties = bindDocProperties();
        if (!docProperties.isEnabled() || !docProperties.getModuleGrouping().isEnabled()) {
            return;
        }

        modulePathsByName().forEach((moduleName, fallbackPaths) -> {
            String beanName = groupedOpenApiBeanName(moduleName);
            if (!registry.containsBeanDefinition(beanName)) {
                registry.registerBeanDefinition(
                        beanName,
                        groupedOpenApiBeanDefinition(moduleName, fallbackPaths));
            }
        });
    }

    private BeanDefinition groupedOpenApiBeanDefinition(String moduleName, List<String> fallbackPaths) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(ModuleGroupedOpenApiFactoryBean.class);
        beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, moduleName);
        beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, fallbackPaths);
        return beanDefinition;
    }

    private Map<String, List<String>> modulePathsByName() {
        Map<String, List<String>> modulePaths = new LinkedHashMap<>();
        metadataLoader.load().forEach(metadata -> {
            List<String> paths = splitPaths(metadata.modulePath());
            if (!paths.isEmpty()) {
                modulePaths.putIfAbsent(metadata.moduleName(), paths);
            }
        });

        ModuleProperties moduleProperties = bindModuleProperties();
        if (moduleProperties.isEnabled()) {
            moduleProperties.getModules().keySet()
                    .forEach(moduleName -> modulePaths.putIfAbsent(moduleName, List.of()));
        }
        return modulePaths;
    }

    private List<String> splitPaths(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        for (String path : value.split(",")) {
            if (!path.isBlank()) {
                paths.add(path.trim());
            }
        }
        return List.copyOf(paths);
    }

    private DocProperties bindDocProperties() {
        if (environment == null) {
            return new DocProperties();
        }
        return Binder.get(environment)
                .bind("mango.doc", DocProperties.class)
                .orElseGet(DocProperties::new);
    }

    private ModuleProperties bindModuleProperties() {
        if (environment == null) {
            return new ModuleProperties();
        }
        return Binder.get(environment)
                .bind("mango.module.module-service", ModuleProperties.class)
                .orElseGet(ModuleProperties::new);
    }

    private String groupedOpenApiBeanName(String moduleName) {
        String encodedModuleName = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(moduleName.getBytes(StandardCharsets.UTF_8));
        return BEAN_NAME_PREFIX + encodedModuleName;
    }
}
