package io.mango.infra.module.starter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Loads Mango module metadata from classpath resources.
 */
public class ModuleMetadataLoader {

    public static final String MODULE_PROPERTIES_LOCATION = "META-INF/mango/module.properties";

    public List<ModuleMetadata> load() {
        List<ModuleMetadata> modules = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> resources = classLoader.getResources(MODULE_PROPERTIES_LOCATION);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                Properties properties = loadProperties(resource);
                String moduleName = properties.getProperty("module-name");
                String modulePath = properties.getProperty("module-path");
                if (moduleName != null && !moduleName.isBlank()) {
                    modules.add(new ModuleMetadata(
                            moduleName.trim(),
                            modulePath == null ? "" : modulePath.trim(),
                            resource.toString()));
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

    public record ModuleMetadata(String moduleName, String modulePath, String source) {
    }
}
