package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.command.ResourceModuleManifestCommand;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Loads the outer module envelopes without deserializing their declaration arrays. */
public final class ResourceManifestArtifactLoader {

    static final String DEFAULT_LOCATION =
            "classpath:META-INF/mango/resource-bootstrap-manifest.json";

    private final ObjectMapper objectMapper;
    private final Resource resource;

    public ResourceManifestArtifactLoader(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper.copy();
        this.resource = resourceLoader.getResource(DEFAULT_LOCATION);
    }

    public Optional<List<ResourceModuleManifestCommand>> load() {
        if (!resource.exists()) {
            return Optional.empty();
        }
        try (InputStream input = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            if (root == null || root.path("schemaVersion").asInt(-1) != 1
                    || !root.path("modules").isArray()) {
                throw new IllegalStateException("Resource build manifest schema is invalid: " + resource);
            }
            List<ResourceModuleManifestCommand> modules = new ArrayList<>();
            for (JsonNode node : root.path("modules")) {
                modules.add(module(node));
            }
            return Optional.of(List.copyOf(modules));
        } catch (IOException exception) {
            throw new IllegalStateException("Read Resource build manifest failed: " + resource, exception);
        }
    }

    private ResourceModuleManifestCommand module(JsonNode node) {
        JsonNode declarations = node.path("declarations");
        JsonNode dependencies = node.path("dependencies");
        if (!node.isObject() || !declarations.isArray() || !dependencies.isArray()) {
            throw new IllegalStateException("Resource build manifest module is invalid: " + resource);
        }
        ResourceModuleManifestCommand module = new ResourceModuleManifestCommand();
        module.setModuleCode(node.path("moduleCode").asText());
        module.setModuleHash(node.path("moduleHash").asText());
        module.setDeclarationCount(node.path("declarationCount").asInt(-1));
        List<String> dependencyCodes = new ArrayList<>();
        dependencies.forEach(dependency -> dependencyCodes.add(dependency.asText()));
        module.setDependencies(dependencyCodes);
        try {
            module.setDeclarations(objectMapper.writeValueAsString(declarations));
        } catch (IOException exception) {
            throw new IllegalStateException("Serialize Resource build manifest module failed: " + resource,
                    exception);
        }
        return module;
    }
}
