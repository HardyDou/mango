package io.mango.plugin.baseline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineArtifactVerifierTest {

    @TempDir
    Path directory;

    @Test
    void rejectsTruncatedBaselineAndUnexpectedBaseline() throws Exception {
        Path baseline = directory.resolve("db/baseline/alpha/B2__baseline.sql");
        Files.createDirectories(baseline.getParent());
        Files.writeString(baseline, """
                -- mango:baseline-idempotent
                CREATE TABLE IF NOT EXISTS alpha_record (id bigint primary key);
                """);
        writeManifest(baseline);
        assertDoesNotThrow(() -> BaselineArtifactVerifier.verify(directory));

        Files.writeString(baseline, "-- mango:baseline-idempotent\nCREATE TABLE");
        MojoExecutionException truncated = assertThrows(
                MojoExecutionException.class,
                () -> BaselineArtifactVerifier.verify(directory));
        assertTrue(truncated.getMessage().contains("checksum mismatch"));

        Files.writeString(baseline, """
                -- mango:baseline-idempotent
                CREATE TABLE IF NOT EXISTS alpha_record (id bigint primary key);
                """);
        writeManifest(baseline);
        Path unexpected = directory.resolve("db/baseline/beta/B1__baseline.sql");
        Files.createDirectories(unexpected.getParent());
        Files.writeString(unexpected, "-- mango:baseline-idempotent\nSELECT 1;");
        MojoExecutionException extra = assertThrows(
                MojoExecutionException.class,
                () -> BaselineArtifactVerifier.verify(directory));
        assertTrue(extra.getMessage().contains("inventory mismatch"));
    }

    private void writeManifest(Path baseline) throws Exception {
        Path manifest = directory.resolve("META-INF/mango/baseline-manifest.json");
        Files.createDirectories(manifest.getParent());
        new ObjectMapper().writeValue(manifest.toFile(), Map.of(
                "formatVersion", "1",
                "modules", List.of(Map.of(
                        "module", "alpha",
                        "baselineResource", "db/baseline/alpha/B2__baseline.sql",
                        "baselineSha256", sha256(Files.readAllBytes(baseline))))));
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
