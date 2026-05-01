package io.mango.infra.doc.starter;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * 根据 Mango 模块元数据动态注册 OpenAPI 分组。
 */
public class ModuleGroupedOpenApiRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final String MODULE_PROPERTIES_LOCATION = "META-INF/mango/module.properties";
    private static final String BEAN_NAME_PREFIX = "mangoDocGroupedOpenApi_";
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        DocProperties properties = bindProperties();
        if (!properties.isEnabled() || !properties.getModuleGrouping().isEnabled()) {
            return;
        }

        Set<String> registeredGroups = new HashSet<>();
        for (ModuleMetadata module : loadModules()) {
            if (!hasText(module.modulePath())) {
                continue;
            }
            String group = module.moduleName();
            if (!registeredGroups.add(group)) {
                continue;
            }
            String beanName = BEAN_NAME_PREFIX + sanitize(group);
            if (registry.containsBeanDefinition(beanName)) {
                continue;
            }
            BeanDefinition beanDefinition = groupedOpenApiBeanDefinition(properties, module);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    private BeanDefinition groupedOpenApiBeanDefinition(DocProperties properties, ModuleMetadata module) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(GroupedOpenApi.class);
        beanDefinition.setInstanceSupplier(() -> buildGroupedOpenApi(properties, module));
        return beanDefinition;
    }

    private GroupedOpenApi buildGroupedOpenApi(DocProperties properties, ModuleMetadata module) {
        String modulePath = normalizePath(module.modulePath());
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder()
                .group(module.moduleName())
                .displayName(module.moduleName() + " (" + modulePath + ")")
                .pathsToMatch(modulePath, modulePath + "/**");
        if (properties.getModuleGrouping().isIncludeScopeTags()) {
            builder.addOperationCustomizer(new MangoApiScopeOperationCustomizer());
        }
        return builder.build();
    }

    private DocProperties bindProperties() {
        if (environment == null) {
            return new DocProperties();
        }
        return Binder.get(environment)
                .bind("mango.doc", DocProperties.class)
                .orElseGet(DocProperties::new);
    }

    private List<ModuleMetadata> loadModules() {
        List<ModuleMetadata> modules = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> resources = classLoader.getResources(MODULE_PROPERTIES_LOCATION);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                Properties properties = loadProperties(resource);
                String moduleName = trim(properties.getProperty("module-name"));
                String modulePath = trim(properties.getProperty("module-path"));
                if (hasText(moduleName)) {
                    modules.add(new ModuleMetadata(moduleName, modulePath));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("加载 Mango 模块元数据失败", e);
        }
        return modules;
    }

    private Properties loadProperties(URL resource) throws IOException {
        try (InputStream inputStream = resource.openStream()) {
            return PropertiesLoaderUtils.loadProperties(new org.springframework.core.io.InputStreamResource(inputStream));
        }
    }

    private String normalizePath(String path) {
        String value = trim(path);
        if (!hasText(value)) {
            return "";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ModuleMetadata(String moduleName, String modulePath) {
    }
}
