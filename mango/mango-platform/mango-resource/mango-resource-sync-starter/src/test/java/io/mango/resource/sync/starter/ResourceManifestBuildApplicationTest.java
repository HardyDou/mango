package io.mango.resource.sync.starter;

import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ResourceManifestBuildApplicationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void dedicatedContextWritesArtifactsAndLoadsOnlyExplicitProviderSources() throws Exception {
        byte[] content = "build-context-asset".getBytes(StandardCharsets.UTF_8);
        Path assetRoot = temporaryDirectory.resolve("assets");
        Files.createDirectories(assetRoot);
        Files.write(assetRoot.resolve("template.bin"), content);
        BuildProviderConfiguration.sha256 = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
        Path output = temporaryDirectory.resolve("classes");

        ResourceManifestBuildApplication.main(new String[]{
                "--mango.resource.registry.artifact-output-directory=" + output,
                "--mango.resource.registry.artifact-context-sources="
                        + BuildProviderConfiguration.class.getName(),
                "--mango.resource.registry.locations=classpath*:does-not-exist/*.json",
                "--mango.file.asset-root=" + assetRoot
        });

        assertThat(Files.readString(output.resolve(
                "META-INF/mango/resource-bootstrap-manifest.json")))
                .contains("build-fixture")
                .contains(BuildProviderConfiguration.sha256);
        assertThat(Files.readString(output.resolve("META-INF/mango/files-manifest.json")))
                .contains("build-fixture/template.bin");
        assertThat(Files.readAllBytes(output.resolve(
                "META-INF/mango/files.bundle/objects/" + BuildProviderConfiguration.sha256)))
                .isEqualTo(content);
    }

    @Test
    void missingOutputDirectoryFailsBeforeCreatingContext() {
        assertThatIllegalStateException()
                .isThrownBy(() -> ResourceManifestBuildApplication.main(new String[0]))
                .withMessageContaining("artifact-output-directory is required");
    }

    @Configuration(proxyBeanMethods = false)
    static class BuildProviderConfiguration {

        private static String sha256;

        @Bean
        ResourceProvider buildFixtureResourceProvider() {
            return () -> List.of(fileAsset());
        }

        private static ResourceDeclaration fileAsset() {
            ResourceDeclaration declaration = new ResourceDeclaration();
            declaration.setId("8510000000000000001");
            declaration.setVersion(1);
            declaration.setResourceType(ResourceTypes.FILE_ASSET);
            declaration.setModuleCode("build-fixture");
            declaration.setBizKey("build-fixture.template");
            declaration.setName("Build fixture template");
            declaration.setTargetModule("mango-file");
            declaration.putField("sha256", value(ResourceFieldType.STRING, sha256));
            declaration.putField("objectName", value(
                    ResourceFieldType.STRING, "build-fixture/template.bin"));
            ResourceField content = value(ResourceFieldType.FILE, null);
            content.setLocation("asset:template.bin");
            content.setMediaType("application/octet-stream");
            declaration.putField("content", content);
            return declaration;
        }

        private static ResourceField value(ResourceFieldType type, Object value) {
            ResourceField field = new ResourceField();
            field.setType(type);
            field.setValue(value);
            return field;
        }
    }
}
