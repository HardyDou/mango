package io.mango.infra.module.starter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.mango.infra.module.api.diagnostic.ModuleInstallation;

import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * 从 classpath 加载 Mango 模块元数据。
 */
public class ModuleMetadataLoader {

    public static final String MODULE_PROPERTIES_LOCATION = "META-INF/mango/module.properties";

    public List<ModuleMetadata> load() {
        List<ModuleMetadata> modules = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ModuleMetadataLoader.class.getClassLoader();
        }
        try {
            Enumeration<URL> resources = classLoader.getResources(MODULE_PROPERTIES_LOCATION);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                Properties properties = loadProperties(resource);
                String moduleName = properties.getProperty("module-name");
                String modulePath = properties.getProperty("module-path");
                if (moduleName != null && !moduleName.isBlank()) {
                    String normalizedModulePath = "";
                    if (modulePath != null) {
                        normalizedModulePath = modulePath.trim();
                    }
                    String persistenceModule = properties.getProperty("diagnostic.persistence-module");
                    String resourceModule = properties.getProperty("diagnostic.resource-module");
                    modules.add(new ModuleMetadata(
                            moduleName.trim(),
                            normalizedModulePath,
                            resource.toString(),
                            resource,
                            normalizeNullable(persistenceModule),
                            normalizeNullable(resourceModule)));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Mango module metadata", e);
        }
        return modules;
    }

    private Properties loadProperties(URL resource) throws IOException {
        try (InputStream inputStream = resource.openStream()) {
            return PropertiesLoaderUtils.loadProperties(new org.springframework.core.io.InputStreamResource(inputStream));
        }
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record ModuleMetadata(
            String moduleName,
            String modulePath,
            String source,
            URL resourceUrl,
            String persistenceModule,
            String resourceModule) {

        public ModuleMetadata(String moduleName, String modulePath, String source) {
            this(moduleName, modulePath, source, null, null, null);
        }

        public String sourceDescription() {
            return source;
        }

        public Map<String, String> diagnosticAttributes() {
            Map<String, String> attributes = new java.util.LinkedHashMap<>();
            if (persistenceModule != null) {
                attributes.put(ModuleInstallation.PERSISTENCE_MODULE_ATTRIBUTE, persistenceModule);
            }
            if (resourceModule != null) {
                attributes.put(ModuleInstallation.RESOURCE_MODULE_ATTRIBUTE, resourceModule);
            }
            return Map.copyOf(attributes);
        }
    }
}
