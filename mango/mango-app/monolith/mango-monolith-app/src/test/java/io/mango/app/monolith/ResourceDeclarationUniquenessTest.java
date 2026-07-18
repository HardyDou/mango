package io.mango.app.monolith;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.support.model.ResourceDeclaration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDeclarationUniquenessTest {

    @Test
    void monolithClasspathResourceIdsAreUniqueWithDemoEnabled() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setDemoEnabled(true);

        List<ResourceDeclaration> declarations = new ResourceDeclarationLoader(
                new ObjectMapper(), properties).load();
        Map<String, List<String>> sourcesById = declarations.stream()
                .collect(Collectors.groupingBy(
                        ResourceDeclaration::getId,
                        TreeMap::new,
                        Collectors.mapping(ResourceDeclaration::getSource, Collectors.toList())));
        sourcesById.entrySet().removeIf(entry -> entry.getValue().size() == 1);

        assertThat(sourcesById)
                .as("resource declaration IDs must be unique across required and demo resources")
                .isEmpty();
    }
}
