package io.mango.resource.support.declaration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.support.config.ResourceRegistryProperties;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

    @Test
    void loadReadsAuthMenuJsonDeclaration() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        Path workflowMenu = findRepositoryFile(
                "mango/mango-platform/mango-workflow/mango-workflow-starter/src/main/resources/"
                        + "META-INF/mango/resources/workflow-common-menu.json");
        properties.setLocations(List.of(workflowMenu.toUri().toString()));

        List<ResourceDeclaration> declarations = new ResourceDeclarationLoader(new ObjectMapper(), properties).load();

        assertThat(declarations).hasSize(1);
        ResourceDeclaration declaration = declarations.get(0);
        assertThat(declaration.getResourceType()).isEqualTo(ResourceTypes.AUTH_MENU);
        assertThat(declaration.getModuleCode()).isEqualTo("workflow");
        assertThat(declaration.getBizKey()).isEqualTo("workflow.menu.internal-admin");
        assertThat(declaration.getFields().get("menus").getType()).isEqualTo(ResourceFieldType.LIST);
        assertThat(declaration.getFields().get("menus").getValue()).asList().hasSize(1);
        Object root = ((List<?>) declaration.getFields().get("menus").getValue()).get(0);
        assertThat(root).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) root).get("menuCode")).isEqualTo("workflow");
    }

    private Path findRepositoryFile(String relativePath) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository file not found: " + relativePath);
    }
}
