package io.mango.resource.sync.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.api.command.ResourceModuleManifestCommand;
import io.mango.resource.support.declaration.ResourceModuleHasher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serializes Resource manifests independently from host Web/Jackson customizations. */
public final class ResourceManifestSerializer {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    public ResourceManifestSerializer() {
    }

    public String serialize(List<ResourceDeclaration> declarations) {
        return serializeValue(declarations);
    }

    public String serializeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Serialize resource manifest failed", exception);
        }
    }

    public List<ResourceModuleManifestCommand> moduleManifests(
            List<ResourceDeclaration> declarations,
            List<String> managedModuleCodes,
            Map<String, List<String>> dependencies,
            ResourceModuleHasher hasher) {
        Map<String, List<ResourceDeclaration>> byModule = new LinkedHashMap<>();
        managedModuleCodes.stream().distinct().sorted()
                .forEach(moduleCode -> byModule.put(moduleCode, new ArrayList<>()));
        declarations.stream().sorted(Comparator.comparing(ResourceDeclaration::getId))
                .forEach(declaration -> byModule.computeIfAbsent(
                        declaration.getModuleCode(), ignored -> new ArrayList<>()).add(declaration));
        return byModule.entrySet().stream().map(entry -> {
            String moduleCode = entry.getKey();
            List<ResourceDeclaration> moduleDeclarations = List.copyOf(entry.getValue());
            List<String> moduleDependencies = dependencies.getOrDefault(moduleCode, List.of()).stream()
                    .distinct().sorted().toList();
            ResourceModuleManifestCommand manifest = new ResourceModuleManifestCommand();
            manifest.setModuleCode(moduleCode);
            manifest.setDependencies(moduleDependencies);
            manifest.setDeclarations(serialize(moduleDeclarations));
            manifest.setDeclarationCount(moduleDeclarations.size());
            manifest.setModuleHash(hasher.hash(moduleCode, moduleDependencies, moduleDeclarations));
            return manifest;
        }).toList();
    }
}
