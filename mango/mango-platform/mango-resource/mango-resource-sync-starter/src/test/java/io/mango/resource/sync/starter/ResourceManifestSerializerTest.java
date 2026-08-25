package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.declaration.ResourceModuleHasher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceManifestSerializerTest {

    @Test
    void serializationIsIndependentFromHostLongIdCustomization() throws Exception {
        ResourceDeclaration declaration = declarationWithLongField();
        ObjectMapper webObjectMapper = JsonMapper.builder()
                .addModule(new SimpleModule().addSerializer(Long.class, ToStringSerializer.instance))
                .build();

        String firstManifest = new ResourceManifestSerializer().serialize(List.of(declaration));
        String secondManifest = new ResourceManifestSerializer().serialize(List.of(declaration));
        String webJson = webObjectMapper.writeValueAsString(List.of(declaration));

        assertThat(firstManifest).isEqualTo(secondManifest)
                .contains("\"value\":2951300000000000003");
        assertThat(webJson).contains("\"value\":\"2951300000000000003\"")
                .isNotEqualTo(firstManifest);
    }

    @Test
    void moduleManifestsAreDeterministicAndIncludeEmptyManagedModules() {
        ResourceDeclaration declaration = declarationWithLongField();
        declaration.setModuleCode("module-b");
        ResourceManifestSerializer serializer = new ResourceManifestSerializer();
        ResourceModuleHasher hasher = new ResourceModuleHasher(
                new ResourceDeclarationCanonicalizer(new ObjectMapper()));

        var first = serializer.moduleManifests(
                List.of(declaration), List.of("module-b", "module-a"),
                Map.of("module-b", List.of("module-a")), hasher);
        var second = serializer.moduleManifests(
                List.of(declaration), List.of("module-a", "module-b"),
                Map.of("module-b", List.of("module-a")), hasher);

        assertThat(first).usingRecursiveComparison().isEqualTo(second);
        assertThat(first).extracting(module -> module.getModuleCode())
                .containsExactly("module-a", "module-b");
        assertThat(first.get(0).getDeclarations()).isEqualTo("[]");
        assertThat(first.get(0).getDeclarationCount()).isZero();
        assertThat(first.get(1).getDependencies()).containsExactly("module-a");
        assertThat(first).allSatisfy(module -> assertThat(module.getModuleHash()).matches("[0-9a-f]{64}"));
    }

    private static ResourceDeclaration declarationWithLongField() {
        ResourceField field = new ResourceField();
        field.setType(ResourceFieldType.LONG);
        field.setValue(2951300000000000003L);
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("resource-1");
        declaration.setVersion(1);
        declaration.setResourceType("AUTH_MENU");
        declaration.putField("businessId", field);
        return declaration;
    }
}
