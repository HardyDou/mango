package io.mango.resource.support.declaration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.support.config.ResourceRegistryProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDeclarationLoaderTest {

    @Test
    void loadReadsYamlAndJsonDeclarations() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of("classpath*:META-INF/mango/resources/test-resource.*"));

        List<ResourceDeclaration> declarations = new ResourceDeclarationLoader(new ObjectMapper(), properties).load();

        assertThat(declarations).hasSize(2);
        ResourceDeclaration message = declarations.stream()
                .filter(resource -> "MESSAGE_TEMPLATE".equals(resource.getResourceType()))
                .findFirst()
                .orElseThrow();
        assertThat(message.getId()).isEqualTo("1900000000000000001");
        assertThat(message.getVersion()).isEqualTo(1);
        assertThat(message.getModuleCode()).isEqualTo("guarantee");
        assertThat(message.getBizKey()).isEqualTo("guarantee.apply.submit");
        assertThat(message.getTargetModule()).isEqualTo("notice");
        assertThat(message.getFields().get("body").getType()).isEqualTo(ResourceFieldType.FILE);

        ResourceDeclaration sequence = declarations.stream()
                .filter(resource -> "SEQUENCE_RULE".equals(resource.getResourceType()))
                .findFirst()
                .orElseThrow();
        assertThat(sequence.getModuleCode()).isEqualTo("finance");
        assertThat(sequence.getVersion()).isEqualTo(2);
    }
}
