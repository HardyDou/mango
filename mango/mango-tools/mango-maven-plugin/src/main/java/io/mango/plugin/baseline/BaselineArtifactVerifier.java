package io.mango.plugin.baseline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

final class BaselineArtifactVerifier {

    private static final String MANIFEST_PATH = "META-INF/mango/baseline-manifest.json";
    private static final String MARKER = "-- mango:baseline-idempotent";
    private static final Pattern BASELINE_FILE_NAME =
            Pattern.compile("B[^/]+__baseline\\.sql");

    private BaselineArtifactVerifier() {
    }

    static void verify(Path generatedResources) throws MojoExecutionException {
        Path root = generatedResources.toAbsolutePath().normalize();
        Path manifestPath = root.resolve(MANIFEST_PATH);
        if (!Files.isRegularFile(manifestPath) || Files.isSymbolicLink(manifestPath)) {
            throw failure("manifest is missing or unsafe: " + manifestPath);
        }
        try {
            Set<Path> expected = expectedBaselines(root, manifestPath);
            verifyInventory(root, expected, actualBaselines(root));
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-038 failed to verify generated baseline artifacts", exception);
        }
    }

    private static Set<Path> expectedBaselines(Path root, Path manifestPath)
            throws IOException, MojoExecutionException {
        JsonNode manifest = new ObjectMapper().readTree(manifestPath.toFile());
        JsonNode modules = manifest.path("modules");
        if (!modules.isArray() || modules.isEmpty()) {
            throw failure("manifest modules must be a non-empty array");
        }
        Set<Path> expected = new LinkedHashSet<>();
        for (JsonNode module : modules) {
            verifyManifestModule(root, module, expected);
        }
        return expected;
    }

    private static void verifyManifestModule(
            Path root,
            JsonNode module,
            Set<Path> expected) throws IOException, MojoExecutionException {
        String moduleName = requiredText(module, "module");
        String resource = requiredText(module, "baselineResource");
        String expectedSha256 = requiredText(module, "baselineSha256");
        Path baseline = validatedBaselinePath(root, moduleName, resource);
        if (!Files.isRegularFile(baseline) || Files.isSymbolicLink(baseline)) {
            throw failure("baseline is missing or unsafe: " + resource);
        }
        byte[] content = Files.readAllBytes(baseline);
        if (!expectedSha256.equals(sha256(content))) {
            throw failure("baseline checksum mismatch: " + resource);
        }
        verifyMarker(content, resource);
        if (!expected.add(baseline)) {
            throw failure("duplicate baseline manifest entry: " + resource);
        }
    }

    private static Path validatedBaselinePath(
            Path root,
            String moduleName,
            String resource) throws MojoExecutionException {
        Path baseline = root.resolve(resource).normalize();
        Path expectedParent = root.resolve("db/baseline").resolve(moduleName).normalize();
        boolean directChild = expectedParent.equals(baseline.getParent());
        boolean validFileName = baseline.getFileName() != null
                && BASELINE_FILE_NAME.matcher(baseline.getFileName().toString()).matches();
        if (!baseline.startsWith(expectedParent) || !directChild || !validFileName) {
            throw failure("invalid baseline path for module " + moduleName + ": " + resource);
        }
        return baseline;
    }

    private static void verifyMarker(byte[] content, String resource)
            throws MojoExecutionException {
        String firstLine = new String(content, StandardCharsets.UTF_8).lines()
                .findFirst()
                .orElse("");
        if (!MARKER.equals(firstLine)) {
            throw failure("baseline marker is missing: " + resource);
        }
    }

    private static Set<Path> actualBaselines(Path root) throws IOException {
        Set<Path> actual = new LinkedHashSet<>();
        Path baselineRoot = root.resolve("db/baseline");
        if (!Files.isDirectory(baselineRoot)) {
            return actual;
        }
        try (var files = Files.walk(baselineRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> BASELINE_FILE_NAME
                            .matcher(path.getFileName().toString()).matches())
                    .map(path -> path.toAbsolutePath().normalize())
                    .forEach(actual::add);
        }
        return actual;
    }

    private static void verifyInventory(Path root, Set<Path> expected, Set<Path> actual)
            throws MojoExecutionException {
        if (actual.equals(expected)) {
            return;
        }
        Set<Path> missing = new LinkedHashSet<>(expected);
        missing.removeAll(actual);
        Set<Path> unexpected = new LinkedHashSet<>(actual);
        unexpected.removeAll(expected);
        throw failure("baseline inventory mismatch; missing=" + relative(root, missing)
                + ", unexpected=" + relative(root, unexpected));
    }

    private static String requiredText(JsonNode node, String field) throws MojoExecutionException {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw failure("manifest module field is missing: " + field);
        }
        return value.asText();
    }

    private static String sha256(byte[] content) throws MojoExecutionException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new MojoExecutionException("SHA-256 is unavailable", exception);
        }
    }

    private static Set<String> relative(Path root, Set<Path> paths) {
        Set<String> relative = new LinkedHashSet<>();
        paths.forEach(path -> relative.add(root.relativize(path).toString().replace('\\', '/')));
        return relative;
    }

    private static MojoExecutionException failure(String message) {
        return new MojoExecutionException("MANGO-BASELINE-039 " + message);
    }
}
