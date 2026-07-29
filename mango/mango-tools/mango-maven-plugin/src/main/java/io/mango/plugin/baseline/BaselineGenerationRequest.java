package io.mango.plugin.baseline;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

record BaselineGenerationRequest(
        String jdbcUrl,
        String username,
        String password,
        String schemaPrefix,
        Path outputDirectory,
        List<String> moduleOrder,
        Map<String, String> moduleGroups,
        boolean keepSchemas) {
}
