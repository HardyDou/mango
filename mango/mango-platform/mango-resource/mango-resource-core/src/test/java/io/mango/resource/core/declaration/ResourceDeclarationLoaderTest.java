package io.mango.resource.support.declaration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.support.config.ResourceRegistryProperties;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void loadDoesNotReadDemoDeclarationsByDefault() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of("classpath*:META-INF/mango/resources/test-resource.*"));

        List<ResourceDeclaration> declarations = new ResourceDeclarationLoader(new ObjectMapper(), properties).load();

        assertThat(declarations)
                .extracting(ResourceDeclaration::getBizKey)
                .doesNotContain("demo.message");
    }

    @Test
    void loadReadsDemoDeclarationsWhenEnabled() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of("classpath*:META-INF/mango/resources/test-resource.*"));
        properties.setDemoEnabled(true);

        List<ResourceDeclaration> declarations = new ResourceDeclarationLoader(new ObjectMapper(), properties).load();

        assertThat(declarations).hasSize(3);
        ResourceDeclaration demo = declarations.stream()
                .filter(resource -> "demo.message".equals(resource.getBizKey()))
                .findFirst()
                .orElseThrow();
        assertThat(demo.getId()).isEqualTo("1900000000000000901");
        assertThat(demo.getResourceType()).isEqualTo("MESSAGE_TEMPLATE");
        assertThat(demo.getModuleCode()).isEqualTo("demo-module");
        assertThat(demo.getTargetModule()).isEqualTo("notice");
    }

    @Test
    void loadAcceptsKebabCaseSyncMode() throws Exception {
        Path declaration = Files.createTempFile("mango-resource-sync-mode", ".yml");
        Files.writeString(declaration, """
                mango:
                  resource:
                    schema-version: 1
                    module-code: test
                    module-name: 测试
                    declarations:
                      MESSAGE_TEMPLATE:
                        - id: "1900000000000000103"
                          version: 1
                          biz-key: test.init-only
                          name: 初始化一次消息
                          target-module: notice
                          sync-mode: init-only
                """);
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of(declaration.toUri().toString()));

        List<ResourceDeclaration> declarations = new ResourceDeclarationLoader(new ObjectMapper(), properties).load();

        assertThat(declarations).hasSize(1);
        assertThat(declarations.get(0).getSyncMode()).isEqualTo(ResourceSyncMode.INIT_ONLY);
    }

    @Test
    void loadAcceptsSnakeCaseSyncMode() throws Exception {
        Path declaration = Files.createTempFile("mango-resource-sync-mode", ".json");
        Files.writeString(declaration, """
                {
                  "mango": {
                    "resource": {
                      "schemaVersion": 1,
                      "moduleCode": "test",
                      "moduleName": "测试",
                      "declarations": {
                        "MESSAGE_TEMPLATE": [
                          {
                            "id": "1900000000000000104",
                            "version": 1,
                            "bizKey": "test.init_only",
                            "name": "初始化一次消息",
                            "targetModule": "notice",
                            "syncMode": "init_only"
                          }
                        ]
                      }
                    }
                  }
                }
                """);
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of(declaration.toUri().toString()));

        List<ResourceDeclaration> declarations = new ResourceDeclarationLoader(new ObjectMapper(), properties).load();

        assertThat(declarations).hasSize(1);
        assertThat(declarations.get(0).getSyncMode()).isEqualTo(ResourceSyncMode.INIT_ONLY);
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

    @Test
    void loadRejectsUnsupportedSchemaVersionWithSourcePath() throws Exception {
        Path declaration = Files.createTempFile("mango-resource-schema", ".json");
        Files.writeString(declaration, """
                {
                  "mango": {
                    "resource": {
                      "schemaVersion": 2,
                      "moduleCode": "test",
                      "moduleName": "测试",
                      "declarations": {
                        "MESSAGE_TEMPLATE": [
                          {
                            "id": "1900000000000000101",
                            "version": 1,
                            "bizKey": "test.message",
                            "name": "测试消息",
                            "targetModule": "notice"
                          }
                        ]
                      }
                    }
                  }
                }
                """);
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of(declaration.toUri().toString()));

        assertThatThrownBy(() -> new ResourceDeclarationLoader(new ObjectMapper(), properties).load())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid resource declaration")
                .hasMessageContaining(declaration.getFileName().toString())
                .hasMessageContaining("Unsupported mango.resource.schemaVersion: 2");
    }

    @Test
    void loadRejectsMissingResourceRootWithSourcePath() throws Exception {
        Path declaration = Files.createTempFile("mango-resource-missing-root", ".json");
        Files.writeString(declaration, """
                {"mango": {}}
                """);
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of(declaration.toUri().toString()));

        assertThatThrownBy(() -> new ResourceDeclarationLoader(new ObjectMapper(), properties).load())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid resource declaration")
                .hasMessageContaining(declaration.getFileName().toString())
                .hasMessageContaining("mango.resource is required");
    }

    @Test
    void loadRejectsInvalidDeclarationWithSourcePathAndDeclarationPath() throws Exception {
        Path declaration = Files.createTempFile("mango-resource-invalid-declaration", ".yml");
        Files.writeString(declaration, """
                mango:
                  resource:
                    schema-version: 1
                    module-code: test
                    module-name: 测试
                    declarations:
                      MESSAGE_TEMPLATE:
                        - id: "1900000000000000102"
                          version: 0
                          name: 测试消息
                          target-module: notice
                """);
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of(declaration.toUri().toString()));

        assertThatThrownBy(() -> new ResourceDeclarationLoader(new ObjectMapper(), properties).load())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid resource declaration")
                .hasMessageContaining(declaration.getFileName().toString())
                .hasMessageContaining("mango.resource.declarations.MESSAGE_TEMPLATE[0].version must be positive");
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
