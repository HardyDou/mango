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
import java.util.List;
import java.util.Map;

/**
 * 加载 classpath JSON/YAML 资源声明。
 */
@RequiredArgsConstructor
public class ResourceDeclarationLoader {

    private final ObjectMapper jsonMapper;
    private final ResourceRegistryProperties properties;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public List<ResourceDeclaration> load() {
        List<ResourceDeclaration> declarations = new ArrayList<>();
        for (String location : properties.getLocations()) {
            if (!StringUtils.hasText(location)) {
                continue;
            }
            declarations.addAll(loadLocation(location));
        }
        return declarations;
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

    private List<ResourceDeclaration> readResource(Resource resource) throws IOException {
        ObjectMapper mapper = chooseMapper(resource);
        try (InputStream inputStream = resource.getInputStream()) {
            ResourceDeclarationFile file = mapper.readValue(inputStream, ResourceDeclarationFile.class);
            return flatten(file, resource.getDescription());
        }
    }

    private ObjectMapper chooseMapper(Resource resource) throws IOException {
        String filename = resource.getFilename();
        if (filename != null && (filename.endsWith(".yml") || filename.endsWith(".yaml"))) {
            return yamlMapper;
        }
        return jsonMapper;
    }

    private List<ResourceDeclaration> flatten(ResourceDeclarationFile file, String source) {
        ResourceDeclarationFile.Resource resource = file.getMango().getResource();
        List<ResourceDeclaration> declarations = new ArrayList<>();
        for (Map.Entry<String, List<ResourceDeclaration>> entry : resource.getDeclarations().entrySet()) {
            for (ResourceDeclaration declaration : entry.getValue()) {
                declaration.setResourceType(entry.getKey());
                declaration.setModuleCode(resource.getModuleCode());
                declaration.setModuleName(resource.getModuleName());
                declaration.setSource(source);
                declarations.add(declaration);
            }
        }
        return declarations;
    }
}
