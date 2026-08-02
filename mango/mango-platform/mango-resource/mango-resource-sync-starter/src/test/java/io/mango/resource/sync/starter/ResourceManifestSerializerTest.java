package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;

import java.util.List;

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
