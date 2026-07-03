package io.mango.resource.support.declaration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.support.config.ResourceRegistryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 加载 classpath JSON/YAML 资源声明。
 */
@RequiredArgsConstructor
public class ResourceDeclarationLoader {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ObjectMapper jsonMapper;
    private final ResourceRegistryProperties properties;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public List<ResourceDeclaration> load() {
        List<ResourceDeclaration> declarations = new ArrayList<>();
        for (String location : resolveLocations()) {
            declarations.addAll(loadLocation(location));
        }
        return declarations;
    }

    private List<String> resolveLocations() {
        Set<String> locations = new LinkedHashSet<>();
        addLocations(locations, properties.getLocations());
        if (properties.isDemoEnabled()) {
            addLocations(locations, properties.getDemoLocations());
        }
        return new ArrayList<>(locations);
    }

    private void addLocations(Set<String> target, List<String> locations) {
        if (locations == null || locations.isEmpty()) {
            return;
        }
        for (String location : locations) {
            if (StringUtils.hasText(location)) {
                target.add(location);
            }
        }
    }

    private List<ResourceDeclaration> loadLocation(String location) {
        try {
            Resource[] resources = resourceResolver.getResources(location);
            List<ResourceDeclaration> declarations = new ArrayList<>();
            for (Resource resource : resources) {
                if (resource.exists() && resource.isReadable()) {
                    declarations.addAll(readResource(resource));
                }
            }
            return declarations;
        } catch (IOException e) {
            throw new IllegalStateException("Load resource declarations failed: " + location, e);
        }
    }

    private List<ResourceDeclaration> readResource(Resource resource) {
        ObjectMapper mapper = chooseMapper(resource);
        String source = resource.getDescription();
        try (InputStream inputStream = resource.getInputStream()) {
            ResourceDeclarationFile file = mapper.readValue(inputStream, ResourceDeclarationFile.class);
            return flatten(file, source);
        } catch (IOException e) {
            throw new IllegalStateException("Read resource declaration failed: " + source, e);
        }
    }

    private ObjectMapper chooseMapper(Resource resource) {
        String filename = resource.getFilename();
        if (filename != null && (filename.endsWith(".yml") || filename.endsWith(".yaml"))) {
            return yamlMapper;
        }
        return jsonMapper;
    }

    private List<ResourceDeclaration> flatten(ResourceDeclarationFile file, String source) {
        validateFile(file, source);
        ResourceDeclarationFile.Resource resource = file.getMango().getResource();
        List<ResourceDeclaration> declarations = new ArrayList<>();
        for (Map.Entry<String, List<ResourceDeclaration>> entry : resource.getDeclarations().entrySet()) {
            String resourceType = entry.getKey();
            List<ResourceDeclaration> typedDeclarations = entry.getValue();
            for (int i = 0; i < typedDeclarations.size(); i++) {
                ResourceDeclaration declaration = typedDeclarations.get(i);
                declaration.setResourceType(entry.getKey());
                declaration.setModuleCode(resource.getModuleCode());
                declaration.setModuleName(resource.getModuleName());
                declaration.setSource(source);
                validateDeclaration(resourceType, declaration, i, source);
                declarations.add(declaration);
            }
        }
        return declarations;
    }

    private void validateFile(ResourceDeclarationFile file, String source) {
        if (file == null || file.getMango() == null || file.getMango().getResource() == null) {
            invalid(source, "mango.resource is required");
        }
        ResourceDeclarationFile.Resource resource = file.getMango().getResource();
        if (resource.getSchemaVersion() == null) {
            invalid(source, "mango.resource.schemaVersion is required");
        }
        if (resource.getSchemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            invalid(source, "Unsupported mango.resource.schemaVersion: " + resource.getSchemaVersion());
        }
        requireText(resource.getModuleCode(), source, "mango.resource.moduleCode is required");
        requireText(resource.getModuleName(), source, "mango.resource.moduleName is required");
        if (resource.getDeclarations() == null || resource.getDeclarations().isEmpty()) {
            invalid(source, "mango.resource.declarations is required");
        }
        for (Map.Entry<String, List<ResourceDeclaration>> entry : resource.getDeclarations().entrySet()) {
            requireText(entry.getKey(), source, "resource type is required");
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                invalid(source, "mango.resource.declarations." + entry.getKey() + " must not be empty");
            }
        }
    }

    private void validateDeclaration(String resourceType, ResourceDeclaration declaration, int index, String source) {
        String path = "mango.resource.declarations." + resourceType + "[" + index + "]";
        if (declaration == null) {
            invalid(source, path + " is required");
        }
        requireText(declaration.getId(), source, path + ".id is required");
        if (declaration.getVersion() == null || declaration.getVersion() < 1) {
            invalid(source, path + ".version must be positive");
        }
        requireText(declaration.getBizKey(), source, path + ".bizKey is required");
        requireText(declaration.getName(), source, path + ".name is required");
        requireText(declaration.getTargetModule(), source, path + ".targetModule is required");
    }

    private void requireText(String value, String source, String message) {
        if (!StringUtils.hasText(value)) {
            invalid(source, message);
        }
    }

    private void invalid(String source, String message) {
        throw new IllegalStateException("Invalid resource declaration: " + source + " - " + message);
    }
}
