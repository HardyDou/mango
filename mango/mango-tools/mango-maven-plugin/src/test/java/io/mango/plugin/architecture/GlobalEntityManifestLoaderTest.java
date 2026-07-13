package io.mango.plugin.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GlobalEntityManifestLoaderTest {

    @Test
    void approvedEntryLoadsExactEntityAndTable(@TempDir Path root) throws Exception {
        Path manifest = root.resolve("business-pmo/global-entity-exceptions.json");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, """
                {
                  "contractId": "global-entity-exceptions",
                  "schemaRevision": 1,
                  "version": 1,
                  "exceptions": [{
                    "entity": "com.example.core.entity.PlatformSettingEntity",
                    "table": "platform_setting",
                    "owner": "platform-team",
                    "reason": "平台级配置不属于任何单一租户",
                    "approvalRef": "ADR-42",
                    "approvedBy": "chief-architect",
                    "expiresOn": "2099-12-31"
                  }]
                }
                """);

        assertEquals(
                Map.of("com.example.core.entity.PlatformSettingEntity", "platform_setting"),
                GlobalEntityManifestLoader.load(root, manifest));
    }

    @Test
    void missingConfiguredManifestFailsClosed(@TempDir Path root) {
        assertThrows(
                MojoExecutionException.class,
                () -> GlobalEntityManifestLoader.load(root, root.resolve("missing.json")));
    }

    @Test
    void missingContractMetadataFailsClosed(@TempDir Path root) throws Exception {
        Path manifest = root.resolve("business-pmo/global-entity-exceptions.json");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, """
                {
                  "version": 1,
                  "exceptions": []
                }
                """);

        assertThrows(
                MojoExecutionException.class,
                () -> GlobalEntityManifestLoader.load(root, manifest));
    }
}
