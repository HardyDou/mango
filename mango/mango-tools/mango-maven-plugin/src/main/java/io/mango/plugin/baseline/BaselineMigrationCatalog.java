package io.mango.plugin.baseline;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.flywaydb.core.api.MigrationVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class BaselineMigrationCatalog {

    private static final Pattern MIGRATION_PATH = Pattern.compile(
            "(?:^|/)db/migration/([a-z0-9][a-z0-9-]*)/(V([^/]+?)__[^/]+\\.sql)$",
            Pattern.CASE_INSENSITIVE);

    private final Map<String, List<MigrationResource>> modules;

    private BaselineMigrationCatalog(Map<String, List<MigrationResource>> modules) {
        this.modules = modules;
    }

    static BaselineMigrationCatalog discover(
            Path searchDirectory,
            MavenProject project,
            Set<String> includedModules) throws MojoExecutionException {
        Map<String, MigrationResource> resources = new LinkedHashMap<>();
        try {
            discoverSourceResources(searchDirectory, resource -> merge(resources, resource));
            discoverArtifactResources(project, resource -> merge(resources, resource));
        } catch (MigrationCollisionException exception) {
            throw new MojoExecutionException(exception.getMessage(), exception);
        }

        Map<String, List<MigrationResource>> byModule = new TreeMap<>();
        for (MigrationResource resource : resources.values()) {
            if (!includedModules.isEmpty() && !includedModules.contains(resource.module())) {
                continue;
            }
            byModule.computeIfAbsent(resource.module(), ignored -> new ArrayList<>()).add(resource);
        }
        if (!includedModules.isEmpty()) {
            Set<String> missing = new LinkedHashSet<>(includedModules);
            missing.removeAll(byModule.keySet());
            if (!missing.isEmpty()) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-002 included modules have no V migrations: " + missing);
            }
        }
        if (byModule.isEmpty()) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-001 no db/migration/<module>/V*.sql resources were discovered");
        }

        Comparator<MigrationResource> comparator = Comparator
                .comparing((MigrationResource resource) -> MigrationVersion.fromVersion(resource.version()))
                .thenComparing(MigrationResource::fileName);
        for (Map.Entry<String, List<MigrationResource>> entry : byModule.entrySet()) {
            entry.getValue().sort(comparator);
            validateVersions(entry.getKey(), entry.getValue());
        }
        Map<String, List<MigrationResource>> immutable = new LinkedHashMap<>();
        byModule.forEach((module, migrations) -> immutable.put(module, List.copyOf(migrations)));
        return new BaselineMigrationCatalog(Collections.unmodifiableMap(immutable));
    }

    Set<String> moduleNames() {
        return modules.keySet();
    }

    List<MigrationResource> migrations(String module) {
        return modules.getOrDefault(module, List.of());
    }

    String latestVersion(String module) {
        List<MigrationResource> migrations = migrations(module);
        return migrations.get(migrations.size() - 1).version();
    }

    String fingerprint(String module) throws MojoExecutionException {
        MessageDigest digest = sha256Digest();
        for (MigrationResource migration : migrations(module)) {
            update(digest, migration.fileName());
            digest.update((byte) 0);
            digest.update(migration.content());
            digest.update((byte) 0);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    Path extractModule(String module, Path root) throws MojoExecutionException {
        Path moduleDirectory = root.resolve(module);
        try {
            Files.createDirectories(moduleDirectory);
            for (MigrationResource migration : migrations(module)) {
                Files.write(moduleDirectory.resolve(migration.fileName()), migration.content());
            }
            return moduleDirectory;
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-007 failed to extract migrations for module " + module,
                    exception);
        }
    }

    Collection<MigrationResource> allMigrations() {
        return modules.values().stream().flatMap(Collection::stream).toList();
    }

    private static void discoverSourceResources(
            Path searchDirectory,
            Consumer<MigrationResource> consumer) throws MojoExecutionException {
        if (searchDirectory == null || !Files.isDirectory(searchDirectory)) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-003 migration search directory does not exist: "
                            + searchDirectory);
        }
        try {
            Files.walkFileTree(searchDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    String name = directory.getFileName() == null
                            ? ""
                            : directory.getFileName().toString();
                    if (!directory.equals(searchDirectory)
                            && (name.equals("target")
                            || name.equals(".git")
                            || name.equals(".runtime")
                            || name.equals("node_modules"))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                        throws IOException {
                    if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sql")) {
                        MigrationResource resource;
                        try {
                            resource = fromPath(
                                    file.toString().replace('\\', '/'),
                                    Files.readAllBytes(file),
                                    file.toString());
                        } catch (MojoExecutionException exception) {
                            throw new IOException(exception);
                        }
                        if (resource != null) {
                            consumer.accept(resource);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-004 failed to scan migration source resources", exception);
        }
    }

    private static void discoverArtifactResources(
            MavenProject project,
            Consumer<MigrationResource> consumer) throws MojoExecutionException {
        if (project == null) {
            return;
        }
        for (Artifact artifact : project.getArtifacts()) {
            Path artifactPath = artifact.getFile() == null ? null : artifact.getFile().toPath();
            if (artifactPath == null || !Files.isRegularFile(artifactPath)) {
                continue;
            }
            String lowerName = artifactPath.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!lowerName.endsWith(".jar") && !lowerName.endsWith(".zip")) {
                continue;
            }
            try (ZipFile zipFile = new ZipFile(artifactPath.toFile())) {
                var entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".sql")) {
                        continue;
                    }
                    try (InputStream input = zipFile.getInputStream(entry)) {
                        MigrationResource resource = fromPath(
                                entry.getName(), input.readAllBytes(), artifactPath + "!/" + entry.getName());
                        if (resource != null) {
                            consumer.accept(resource);
                        }
                    }
                }
            } catch (IOException exception) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-005 failed to scan dependency artifact " + artifactPath,
                        exception);
            }
        }
    }

    private static MigrationResource fromPath(String path, byte[] content, String source)
            throws MojoExecutionException {
        Matcher matcher = MIGRATION_PATH.matcher(path);
        if (!matcher.find()) {
            return null;
        }
        String module = matcher.group(1).toLowerCase(Locale.ROOT);
        String fileName = matcher.group(2);
        String version = matcher.group(3);
        if (version.isBlank()) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-006 migration has an empty version: " + source);
        }
        return new MigrationResource(module, version, fileName, content, source);
    }

    private static void merge(Map<String, MigrationResource> resources, MigrationResource candidate) {
        String key = candidate.module() + "/" + candidate.fileName().toLowerCase(Locale.ROOT);
        MigrationResource existing = resources.putIfAbsent(key, candidate);
        if (existing != null && !MessageDigest.isEqual(existing.content(), candidate.content())) {
            throw new MigrationCollisionException(existing, candidate);
        }
    }

    private static void validateVersions(String module, List<MigrationResource> migrations)
            throws MojoExecutionException {
        Map<MigrationVersion, MigrationResource> versions = new LinkedHashMap<>();
        for (MigrationResource migration : migrations) {
            MigrationVersion version;
            try {
                version = MigrationVersion.fromVersion(migration.version());
            } catch (RuntimeException exception) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-008 invalid migration version " + migration.fileName(),
                        exception);
            }
            MigrationResource previous = versions.putIfAbsent(version, migration);
            if (previous != null) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-009 duplicate migration version in module " + module
                                + ": " + previous.fileName() + " and " + migration.fileName());
            }
        }
    }

    private static MessageDigest sha256Digest() throws MojoExecutionException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new MojoExecutionException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    record MigrationResource(
            String module,
            String version,
            String fileName,
            byte[] content,
            String source) {
        MigrationResource {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    private static final class MigrationCollisionException extends RuntimeException {
        private MigrationCollisionException(MigrationResource existing, MigrationResource candidate) {
            super("MANGO-BASELINE-010 migration resource collision: "
                    + existing.module() + "/" + existing.fileName()
                    + "; sources=" + existing.source() + ", " + candidate.source());
        }
    }
}
