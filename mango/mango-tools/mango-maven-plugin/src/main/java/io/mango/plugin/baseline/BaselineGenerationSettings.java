package io.mango.plugin.baseline;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class BaselineGenerationSettings {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String schemaPrefix;
    private final MySqlSchemaDefaults schemaDefaults;
    private final Path outputDirectory;
    private final List<String> moduleOrder;
    private final Map<String, String> moduleGroups;
    private final boolean keepSchemas;
    private final ResourceBaselineExecutionSettings resourceBaseline;

    BaselineGenerationSettings(
            String jdbcUrl,
            String username,
            String password,
            String schemaPrefix,
            MySqlSchemaDefaults schemaDefaults,
            Path outputDirectory,
            List<String> moduleOrder,
            Map<String, String> moduleGroups,
            boolean keepSchemas) {
        this(jdbcUrl, username, password, schemaPrefix, schemaDefaults, outputDirectory,
                moduleOrder, moduleGroups, keepSchemas, null);
    }

    BaselineGenerationSettings(
            String jdbcUrl,
            String username,
            String password,
            String schemaPrefix,
            MySqlSchemaDefaults schemaDefaults,
            Path outputDirectory,
            List<String> moduleOrder,
            Map<String, String> moduleGroups,
            boolean keepSchemas,
            ResourceBaselineExecutionSettings resourceBaseline) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.schemaPrefix = schemaPrefix;
        this.schemaDefaults = schemaDefaults;
        this.outputDirectory = outputDirectory;
        this.moduleOrder = List.copyOf(moduleOrder);
        this.moduleGroups = Map.copyOf(moduleGroups);
        this.keepSchemas = keepSchemas;
        this.resourceBaseline = resourceBaseline;
    }

    String jdbcUrl() {
        return jdbcUrl;
    }

    String username() {
        return username;
    }

    String password() {
        return password;
    }

    String schemaPrefix() {
        return schemaPrefix;
    }

    MySqlSchemaDefaults schemaDefaults() {
        return schemaDefaults;
    }

    Path outputDirectory() {
        return outputDirectory;
    }

    List<String> moduleOrder() {
        return moduleOrder;
    }

    Map<String, String> moduleGroups() {
        return moduleGroups;
    }

    boolean keepSchemas() {
        return keepSchemas;
    }

    ResourceBaselineExecutionSettings resourceBaseline() {
        return resourceBaseline;
    }

    boolean resourceBaselineEnabled() {
        return resourceBaseline != null;
    }
}
