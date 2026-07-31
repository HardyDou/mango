package io.mango.plugin.baseline;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class BaselineGenerationSettings {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String schemaPrefix;
    private final Path outputDirectory;
    private final List<String> moduleOrder;
    private final Map<String, String> moduleGroups;
    private final boolean keepSchemas;

    BaselineGenerationSettings(
            String jdbcUrl,
            String username,
            String password,
            String schemaPrefix,
            Path outputDirectory,
            List<String> moduleOrder,
            Map<String, String> moduleGroups,
            boolean keepSchemas) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.schemaPrefix = schemaPrefix;
        this.outputDirectory = outputDirectory;
        this.moduleOrder = List.copyOf(moduleOrder);
        this.moduleGroups = Map.copyOf(moduleGroups);
        this.keepSchemas = keepSchemas;
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
}
