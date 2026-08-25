package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ResourceManifestArtifactWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesDeterministicModuleManifestAndContentAddressedFileBundle() throws Exception {
        Path assetRoot = temporaryDirectory.resolve("assets");
        Files.createDirectories(assetRoot);
        byte[] content = "issue-851-file-asset".getBytes(StandardCharsets.UTF_8);
        Files.write(assetRoot.resolve("template.docx"), content);
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        ResourceDeclaration declaration = fileAsset(sha256);
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("provider", (ResourceProvider) () -> List.of(declaration));
        ResourceDeclarationCollector collector = new ResourceDeclarationCollector(
                beans.getBeanProvider(ResourceProvider.class));
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mango.file.asset-root", assetRoot.toString());
        ResourceManifestArtifactWriter writer = new ResourceManifestArtifactWriter(
                collector, new ResourceManifestSerializer(),
                new ResourceDeclarationCanonicalizer(new ObjectMapper()),
                new DefaultResourceLoader(), environment);
        Path firstOutput = temporaryDirectory.resolve("first");
        Path secondOutput = temporaryDirectory.resolve("second");

        var first = writer.write(firstOutput);
        var second = writer.write(secondOutput);

        assertThat(first).isEqualTo(second);
        assertThat(first.moduleCount()).isEqualTo(1);
        assertThat(first.declarationCount()).isEqualTo(1);
        assertThat(first.fileObjectCount()).isEqualTo(1);
        assertThat(Files.readString(firstOutput.resolve("META-INF/mango/resource-bootstrap-manifest.json")))
                .isEqualTo(Files.readString(secondOutput.resolve(
                        "META-INF/mango/resource-bootstrap-manifest.json")))
                .contains("\"moduleCode\":\"guarantee\"")
                .contains("classpath:META-INF/mango/files.bundle/objects/" + sha256)
                .doesNotContain("asset:template.docx")
                .containsPattern("\"moduleHash\":\"[0-9a-f]{64}\"");
        assertThat(Files.readString(firstOutput.resolve("META-INF/mango/files-manifest.json")))
                .isEqualTo(Files.readString(secondOutput.resolve("META-INF/mango/files-manifest.json")))
                .contains(sha256)
                .contains("mango-assets/guarantee/template.docx");
        assertThat(Files.readAllBytes(firstOutput.resolve(
                "META-INF/mango/files.bundle/objects/" + sha256))).isEqualTo(content);
    }

    @Test
    void rejectsChecksumMismatchWithoutPublishingPartialObject() throws Exception {
        Path assetRoot = temporaryDirectory.resolve("mismatch-assets");
        Files.createDirectories(assetRoot);
        Files.writeString(assetRoot.resolve("template.docx"), "actual-content", StandardCharsets.UTF_8);
        Path output = temporaryDirectory.resolve("mismatch-output");

        assertThatIllegalStateException()
                .isThrownBy(() -> writer(assetRoot, List.of(fileAsset("0".repeat(64)))).write(output))
                .withMessageContaining("FILE_ASSET sha256 mismatch");

        Path objects = output.resolve("META-INF/mango/files.bundle/objects");
        if (Files.exists(objects)) {
            try (var files = Files.list(objects)) {
                assertThat(files.toList()).isEmpty();
            }
        }
    }

    @Test
    void rejectsUnsafeAssetPathsAtBuildTime() throws Exception {
        Path assetRoot = temporaryDirectory.resolve("safe-assets");
        Files.createDirectories(assetRoot);
        Files.writeString(assetRoot.resolve("template.docx"), "safe", StandardCharsets.UTF_8);
        ResourceDeclaration traversal = fileAsset("0".repeat(64));
        traversal.getFields().get("content").setLocation("asset:../outside.docx");

        assertThatIllegalStateException()
                .isThrownBy(() -> writer(assetRoot, List.of(traversal))
                        .write(temporaryDirectory.resolve("unsafe-output")))
                .withMessageContaining("asset path is unsafe");
    }

    @Test
    void storesIdenticalFileContentAsOnePhysicalObject() throws Exception {
        Path assetRoot = temporaryDirectory.resolve("deduplicated-assets");
        Files.createDirectories(assetRoot);
        byte[] content = "shared-content".getBytes(StandardCharsets.UTF_8);
        Files.write(assetRoot.resolve("template.docx"), content);
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        ResourceDeclaration first = fileAsset(sha256);
        ResourceDeclaration second = fileAsset(sha256);
        second.setId("2951300000000000004");
        second.setBizKey("guarantee.template.copy");
        second.getFields().get("objectName").setValue("mango-assets/guarantee/template-copy.docx");
        Path output = temporaryDirectory.resolve("deduplicated-output");

        var result = writer(assetRoot, List.of(first, second)).write(output);

        assertThat(result.declarationCount()).isEqualTo(2);
        assertThat(result.fileObjectCount()).isEqualTo(2);
        try (var files = Files.list(output.resolve("META-INF/mango/files.bundle/objects"))) {
            assertThat(files.filter(Files::isRegularFile).toList())
                    .containsExactly(output.resolve("META-INF/mango/files.bundle/objects/" + sha256));
        }
    }

    private ResourceManifestArtifactWriter writer(Path assetRoot, List<ResourceDeclaration> declarations) {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("provider", (ResourceProvider) () -> declarations);
        ResourceDeclarationCollector collector = new ResourceDeclarationCollector(
                beans.getBeanProvider(ResourceProvider.class));
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mango.file.asset-root", assetRoot.toString());
        return new ResourceManifestArtifactWriter(
                collector, new ResourceManifestSerializer(),
                new ResourceDeclarationCanonicalizer(new ObjectMapper()),
                new DefaultResourceLoader(), environment);
    }

    private static ResourceDeclaration fileAsset(String sha256) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2951300000000000003");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.FILE_ASSET);
        declaration.setModuleCode("guarantee");
        declaration.setBizKey("guarantee.template");
        declaration.setName("Guarantee template");
        declaration.setTargetModule("mango-file");
        declaration.putField("sha256", value(ResourceFieldType.STRING, sha256));
        declaration.putField("objectName", value(
                ResourceFieldType.STRING, "mango-assets/guarantee/template.docx"));
        ResourceField content = value(ResourceFieldType.FILE, null);
        content.setLocation("asset:template.docx");
        content.setMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
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
