package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ResourceManifestArtifactLoaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loadsOuterEnvelopeAndPreservesDeclarationsAsOpaqueJson() {
        String manifest = """
                {"schemaVersion":1,"modules":[{
                  "moduleCode":"guarantee",
                  "moduleHash":"%s",
                  "dependencies":["file"],
                  "declarationCount":1,
                  "declarations":[{"id":"851","name":"fixture"}]
                }]}
                """.formatted("a".repeat(64));
        ResourceManifestArtifactLoader loader = loader(manifest);

        var modules = loader.load().orElseThrow();

        assertThat(modules).hasSize(1);
        assertThat(modules.get(0).getModuleCode()).isEqualTo("guarantee");
        assertThat(modules.get(0).getDependencies()).containsExactly("file");
        assertThat(modules.get(0).getDeclarationCount()).isEqualTo(1);
        assertThat(modules.get(0).getDeclarations()).contains("\"id\":\"851\"");
    }

    @Test
    void rejectsUnknownSchema() {
        assertThatIllegalStateException()
                .isThrownBy(() -> loader("{\"schemaVersion\":2,\"modules\":[]}").load())
                .withMessageContaining("schema is invalid");
    }

    @Test
    void rejectsMalformedOuterAndModuleStructures() {
        assertThatIllegalStateException()
                .isThrownBy(() -> loader("{\"schemaVersion\":1,\"modules\":{}}").load())
                .withMessageContaining("schema is invalid");
        assertThatIllegalStateException()
                .isThrownBy(() -> loader("{\"schemaVersion\":1,\"modules\":[[]]}").load())
                .withMessageContaining("module is invalid");
        assertThatIllegalStateException()
                .isThrownBy(() -> loader("""
                        {"schemaVersion":1,"modules":[{
                          "moduleCode":"guarantee","moduleHash":"%s",
                          "dependencies":{},"declarationCount":0,"declarations":[]
                        }]}
                        """.formatted("a".repeat(64))).load())
                .withMessageContaining("module is invalid");
    }

    @Test
    void rejectsUnreadableJsonInsteadOfTreatingItAsNoArtifact() {
        assertThatIllegalStateException()
                .isThrownBy(() -> loader("{not-json").load())
                .withMessageContaining("Read Resource build manifest failed");
    }

    private ResourceManifestArtifactLoader loader(String content) {
        return new ResourceManifestArtifactLoader(objectMapper, resourceLoader(content));
    }

    private static ResourceLoader resourceLoader(String content) {
        return new ResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public ClassLoader getClassLoader() {
                return ResourceManifestArtifactLoaderTest.class.getClassLoader();
            }
        };
    }
}
