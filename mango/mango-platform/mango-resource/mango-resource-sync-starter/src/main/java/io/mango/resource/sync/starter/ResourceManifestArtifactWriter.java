package io.mango.resource.sync.starter;

import io.mango.resource.api.command.ResourceModuleManifestCommand;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceModuleHasher;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes deterministic Resource and content-addressed file manifests for a build artifact. */
public final class ResourceManifestArtifactWriter {

    private static final String RESOURCE_MANIFEST = "META-INF/mango/resource-bootstrap-manifest.json";
    private static final String FILE_MANIFEST = "META-INF/mango/files-manifest.json";
    private static final String OBJECT_DIRECTORY = "META-INF/mango/files.bundle/objects";

    private final ResourceDeclarationCollector collector;
    private final ResourceManifestSerializer serializer;
    private final ResourceModuleHasher moduleHasher;
    private final ResourceLoader resourceLoader;
    private final Environment environment;

    public ResourceManifestArtifactWriter(ResourceDeclarationCollector collector,
                                          ResourceManifestSerializer serializer,
                                          ResourceDeclarationCanonicalizer canonicalizer,
                                          ResourceLoader resourceLoader,
                                          Environment environment) {
        this.collector = collector;
        this.serializer = serializer;
        this.moduleHasher = new ResourceModuleHasher(canonicalizer);
        this.resourceLoader = resourceLoader;
        this.environment = environment;
    }

    public ResourceArtifactResult write(Path outputDirectory) {
        List<ResourceDeclaration> declarations = collector.collectBootstrap().stream()
                .sorted(Comparator.comparing(ResourceDeclaration::getId)).toList();
        MaterializedFiles materialized = materializeFiles(outputDirectory, declarations);
        List<ResourceDeclaration> packagedDeclarations = materialized.declarations();
        List<String> moduleCodes = collector.managedBootstrapModuleCodes(packagedDeclarations).stream().sorted().toList();
        List<ResourceModuleManifestCommand> modules = serializer.moduleManifests(
                packagedDeclarations, moduleCodes, collector.managedBootstrapModuleDependencies(), moduleHasher);
        Map<String, Object> resourceManifest = new LinkedHashMap<>();
        resourceManifest.put("schemaVersion", 1);
        resourceManifest.put("modules", modules.stream().map(this::moduleEntry).toList());
        Map<String, Object> fileManifest = new LinkedHashMap<>();
        fileManifest.put("schemaVersion", 1);
        fileManifest.put("objects", materialized.files());
        writeUtf8(outputDirectory.resolve(RESOURCE_MANIFEST), serializer.serializeValue(resourceManifest));
        writeUtf8(outputDirectory.resolve(FILE_MANIFEST), serializer.serializeValue(fileManifest));
        return new ResourceArtifactResult(modules.size(), declarations.size(), materialized.files().size());
    }

    private Map<String, Object> moduleEntry(ResourceModuleManifestCommand module) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("moduleCode", module.getModuleCode());
        entry.put("moduleHash", module.getModuleHash());
        entry.put("dependencies", module.getDependencies());
        entry.put("declarationCount", module.getDeclarationCount());
        try {
            entry.put("declarations", new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(module.getDeclarations()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Parse generated Resource module failed: " + module.getModuleCode(),
                    exception);
        }
        return entry;
    }

    private MaterializedFiles materializeFiles(Path outputDirectory,
                                               List<ResourceDeclaration> declarations) {
        List<Map<String, Object>> files = new ArrayList<>();
        List<ResourceDeclaration> packagedDeclarations = new ArrayList<>();
        for (ResourceDeclaration declaration : declarations) {
            if (!ResourceTypes.FILE_ASSET.equals(declaration.getResourceType())) {
                packagedDeclarations.add(declaration);
                continue;
            }
            ResourceField contentField = declaration.getFields().get("content");
            String location = contentField == null ? null : contentField.getLocation();
            String expectedHash = fieldText(declaration, "sha256");
            Resource content = resolveContent(location);
            Path objectPath = outputDirectory.resolve(OBJECT_DIRECTORY).resolve(expectedHash);
            ContentIdentity identity = copyAndDigest(content, objectPath, expectedHash, declaration.getId());
            if (!expectedHash.equals(identity.sha256())) {
                throw new IllegalStateException("FILE_ASSET sha256 mismatch: " + declaration.getId());
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("resourceId", declaration.getId());
            entry.put("moduleCode", declaration.getModuleCode());
            entry.put("objectKey", fieldText(declaration, "objectName"));
            entry.put("sha256", identity.sha256());
            entry.put("size", identity.size());
            entry.put("mime", StringUtils.hasText(contentField.getMediaType())
                    ? contentField.getMediaType().trim() : "application/octet-stream");
            entry.put("source", location);
            files.add(entry);
            ResourceDeclaration packaged = declaration.copy();
            ResourceField packagedContent = copyField(contentField);
            packagedContent.setLocation("classpath:" + OBJECT_DIRECTORY + "/" + expectedHash);
            packaged.putField("content", packagedContent);
            packagedDeclarations.add(packaged);
        }
        files.sort(Comparator.comparing(entry -> String.valueOf(entry.get("resourceId"))));
        packagedDeclarations.sort(Comparator.comparing(ResourceDeclaration::getId));
        return new MaterializedFiles(List.copyOf(packagedDeclarations), List.copyOf(files));
    }

    private static ResourceField copyField(ResourceField source) {
        ResourceField copy = new ResourceField();
        copy.setType(source.getType());
        copy.setValue(source.getValue());
        copy.setLocation(source.getLocation());
        copy.setEncoding(source.getEncoding());
        copy.setMediaType(source.getMediaType());
        return copy;
    }

    private Resource resolveContent(String location) {
        if (!StringUtils.hasText(location)) {
            throw new IllegalStateException("FILE_ASSET content location is required");
        }
        String normalized = location.trim();
        if (normalized.startsWith("classpath:")) {
            return resourceLoader.getResource(normalized);
        }
        if (!normalized.startsWith("asset:")) {
            throw new IllegalStateException("FILE_ASSET content location is unsupported: " + normalized);
        }
        String configuredRoot = environment.getProperty("mango.file.asset-root");
        if (!StringUtils.hasText(configuredRoot)) {
            throw new IllegalStateException("mango.file.asset-root is required for " + normalized);
        }
        try {
            Path root = Path.of(configuredRoot.trim()).toRealPath();
            Path relative = Path.of(normalized.substring("asset:".length()));
            if (relative.isAbsolute() || relative.toString().contains("..")) {
                throw new IllegalStateException("FILE_ASSET asset path is unsafe: " + normalized);
            }
            Path resolved = root.resolve(relative).normalize().toRealPath();
            if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
                throw new IllegalStateException("FILE_ASSET asset path escapes root: " + normalized);
            }
            return new FileSystemResource(resolved);
        } catch (IOException | InvalidPathException exception) {
            throw new IllegalStateException("FILE_ASSET asset is unreadable: " + normalized, exception);
        }
    }

    private static ContentIdentity copyAndDigest(Resource resource, Path target,
                                                 String expectedHash, String resourceId) {
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("FILE_ASSET content is unreadable: " + resource);
        }
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), ".object-", ".tmp");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size;
            try (InputStream input = new DigestInputStream(resource.getInputStream(), digest)) {
                size = Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha256 = HexFormat.of().formatHex(digest.digest());
            if (!expectedHash.equals(sha256)) {
                throw new IllegalStateException("FILE_ASSET sha256 mismatch: " + resourceId);
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            return new ContentIdentity(sha256, size);
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Materialize FILE_ASSET failed: " + resource, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The build already has a more useful primary failure; temporary cleanup is best effort.
                }
            }
        }
    }

    private static void writeUtf8(Path target, String content) {
        try {
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), ".manifest-", ".tmp");
            Files.writeString(temporary, content, java.nio.charset.StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("Write Resource build manifest failed: " + target, exception);
        }
    }

    private static String fieldText(ResourceDeclaration declaration, String fieldName) {
        ResourceField field = declaration.getFields().get(fieldName);
        if (field == null || field.getValue() == null || !StringUtils.hasText(String.valueOf(field.getValue()))) {
            throw new IllegalStateException("FILE_ASSET field is required: " + fieldName);
        }
        return String.valueOf(field.getValue()).trim();
    }

    public record ResourceArtifactResult(int moduleCount, int declarationCount, int fileObjectCount) {
    }

    private record ContentIdentity(String sha256, long size) {
    }

    private record MaterializedFiles(List<ResourceDeclaration> declarations,
                                     List<Map<String, Object>> files) {
    }
}
