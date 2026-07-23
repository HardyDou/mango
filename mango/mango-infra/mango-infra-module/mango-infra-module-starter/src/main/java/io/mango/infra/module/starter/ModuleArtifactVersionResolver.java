package io.mango.infra.module.starter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Resolves actual build version from the artifact that owns a module metadata resource.
 */
public class ModuleArtifactVersionResolver {

    private static final int MAX_POM_PROPERTIES_BYTES = 64 * 1024;
    private static final int MAX_POM_CANDIDATES = 256;
    private static final int MAX_VERSION_LENGTH = 256;

    /**
     * Resolves Maven pom.properties first, then the owning JAR manifest.
     */
    public VersionResult resolve(URL moduleMetadataResource, String moduleCode) {
        if (moduleMetadataResource == null || !"jar".equals(moduleMetadataResource.getProtocol())) {
            return VersionResult.unknown();
        }
        try {
            URLConnection connection = moduleMetadataResource.openConnection();
            connection.setUseCaches(false);
            if (!(connection instanceof JarURLConnection jarConnection)) {
                return resolveNestedManifest(moduleMetadataResource);
            }
            try (JarFile jarFile = jarConnection.getJarFile()) {
                VersionResult maven = resolveMavenVersion(jarFile, moduleCode);
                if (maven.version() != null) {
                    return maven;
                }
                return resolveManifestVersion(jarFile.getManifest());
            }
        } catch (IOException | RuntimeException exception) {
            return VersionResult.unknown();
        }
    }

    private VersionResult resolveMavenVersion(JarFile jarFile, String moduleCode) throws IOException {
        List<JarEntry> candidates = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith("META-INF/maven/")
                    && entry.getName().endsWith("/pom.properties")) {
                candidates.add(entry);
                if (candidates.size() > MAX_POM_CANDIDATES) {
                    return VersionResult.unknown();
                }
            }
        }
        candidates.sort(Comparator.comparing(JarEntry::getName));
        VersionResult fallback = VersionResult.unknown();
        String normalizedModuleCode = moduleCode == null ? "" : moduleCode.replace("mango-", "");
        for (JarEntry candidate : candidates) {
            Properties properties = loadBoundedProperties(jarFile, candidate);
            if (properties == null) {
                continue;
            }
            String groupId = properties.getProperty("groupId", "");
            String artifactId = properties.getProperty("artifactId", "");
            String version = properties.getProperty("version");
            if (!isBoundedVersion(version)) {
                continue;
            }
            VersionResult result = new VersionResult(version.trim(), "MAVEN_POM_PROPERTIES");
            if (fallback.version() == null && groupId.startsWith("io.mango")) {
                fallback = result;
            }
            if (!normalizedModuleCode.isEmpty()
                    && groupId.startsWith("io.mango")
                    && artifactId.contains(normalizedModuleCode)) {
                return result;
            }
        }
        return fallback;
    }

    private Properties loadBoundedProperties(JarFile jarFile, JarEntry candidate) throws IOException {
        if (candidate.getSize() > MAX_POM_PROPERTIES_BYTES) {
            return null;
        }
        try (InputStream inputStream = jarFile.getInputStream(candidate)) {
            byte[] content = inputStream.readNBytes(MAX_POM_PROPERTIES_BYTES + 1);
            if (content.length > MAX_POM_PROPERTIES_BYTES) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(content));
            return properties;
        }
    }

    private VersionResult resolveNestedManifest(URL moduleMetadataResource) {
        try {
            URL manifestResource = new URL(moduleMetadataResource, "../MANIFEST.MF");
            URLConnection connection = manifestResource.openConnection();
            connection.setUseCaches(false);
            try (InputStream inputStream = connection.getInputStream()) {
                return resolveManifestVersion(new Manifest(inputStream));
            }
        } catch (IOException | RuntimeException exception) {
            return VersionResult.unknown();
        }
    }

    private VersionResult resolveManifestVersion(Manifest manifest) {
        if (manifest == null) {
            return VersionResult.unknown();
        }
        Attributes attributes = manifest.getMainAttributes();
        String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        if (version == null || version.isBlank()) {
            version = attributes.getValue("Build-Version");
        }
        return !isBoundedVersion(version)
                ? VersionResult.unknown()
                : new VersionResult(version.trim(), "JAR_MANIFEST");
    }

    private boolean isBoundedVersion(String version) {
        return version != null && !version.isBlank() && version.length() <= MAX_VERSION_LENGTH;
    }

    /** Version and stable evidence source. */
    public record VersionResult(String version, String source) {
        static VersionResult unknown() {
            return new VersionResult(null, "UNKNOWN");
        }
    }
}
