package io.mango.infra.persistence.starter.datasource;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * 从模块元数据加载默认逻辑数据源。
 */
public class PersistenceModuleDataSourceDefaults {

    static final String MODULE_PROPERTIES_LOCATION = "META-INF/mango/module.properties";

    static final String MODULE_NAME_PROPERTY = "module-name";

    static final String PERSISTENCE_DATASOURCE_PROPERTY = "persistence-datasource";

    private final Map<String, String> defaults;

    public PersistenceModuleDataSourceDefaults() {
        this(loadDefaults());
    }

    public PersistenceModuleDataSourceDefaults(Map<String, String> defaults) {
        this.defaults = Collections.unmodifiableMap(new LinkedHashMap<>(defaults));
    }

    /**
     * 查询模块默认逻辑数据源。
     */
    public Optional<String> resolve(String moduleName) {
        if (!StringUtils.hasText(moduleName)) {
            return Optional.empty();
        }
        return Optional.ofNullable(defaults.get(moduleName.trim()));
    }

    /**
     * 所有模块默认逻辑数据源。
     */
    public Map<String, String> defaults() {
        return defaults;
    }

    private static Map<String, String> loadDefaults() {
        Map<String, String> result = new LinkedHashMap<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> resources = classLoader.getResources(MODULE_PROPERTIES_LOCATION);
            while (resources.hasMoreElements()) {
                loadDefault(resources.nextElement()).ifPresent(defaultValue ->
                        result.put(defaultValue.moduleName(), defaultValue.datasource()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Mango persistence module datasource defaults", e);
        }
        return result;
    }

    private static Optional<ModuleDefaultDataSource> loadDefault(URL resource) throws IOException {
        Properties properties = loadProperties(resource);
        String moduleName = properties.getProperty(MODULE_NAME_PROPERTY);
        String datasource = properties.getProperty(PERSISTENCE_DATASOURCE_PROPERTY);
        if (!StringUtils.hasText(moduleName) || !StringUtils.hasText(datasource)) {
            return Optional.empty();
        }
        return Optional.of(new ModuleDefaultDataSource(moduleName.trim(), datasource.trim()));
    }

    private static Properties loadProperties(URL resource) throws IOException {
        try (InputStream inputStream = resource.openStream()) {
            return PropertiesLoaderUtils.loadProperties(new InputStreamResource(inputStream));
        }
    }

    private record ModuleDefaultDataSource(String moduleName, String datasource) {
    }
}
