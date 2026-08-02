package io.mango.resource.sync.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mango.resource.support.model.ResourceDeclaration;

import java.util.List;

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
        try {
            return objectMapper.writeValueAsString(declarations);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Serialize resource declarations failed", exception);
        }
    }
}
