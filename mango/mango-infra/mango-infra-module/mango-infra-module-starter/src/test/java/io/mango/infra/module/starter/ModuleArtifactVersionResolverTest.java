package io.mango.infra.module.starter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleArtifactVersionResolverTest {

    @TempDir
    Path temporaryDirectory;

    private final ModuleArtifactVersionResolver resolver = new ModuleArtifactVersionResolver();

    @Test
    void resolvesPomPropertiesFromOwningJar() throws Exception {
        Path jar = temporaryDirectory.resolve("mango-link-starter.jar");
        Manifest manifest = manifest("manifest-fallback");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            writeEntry(output, "META-INF/mango/module.properties", "module-name=mango-link\n");
            writeEntry(output, "META-INF/maven/io.mango.platform.link/mango-link-starter/pom.properties",
                    "groupId=io.mango.platform.link\nartifactId=mango-link-starter\nversion=1.2.3\n");
        }
        URL metadata = new URL("jar:" + jar.toUri().toURL() + "!/META-INF/mango/module.properties");

        var result = resolver.resolve(metadata, "mango-link");

        assertThat(result.version()).isEqualTo("1.2.3");
        assertThat(result.source()).isEqualTo("MAVEN_POM_PROPERTIES");
    }

    @Test
    void nonJarUrlConnectionCanResolveNestedManifestWithoutUnsafeCast() throws Exception {
        byte[] manifest = manifestBytes("2.0.0-nested");
        URL nestedMetadata = new URL(
                null,
                "jar:nested:/application.jar/!BOOT-INF/lib/mango-link.jar!/META-INF/mango/module.properties",
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) {
                        return new URLConnection(url) {
                            @Override
                            public void connect() {
                                connected = true;
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                if (url.toExternalForm().endsWith("/META-INF/MANIFEST.MF")) {
                                    return new ByteArrayInputStream(manifest);
                                }
                                throw new IOException("resource unavailable");
                            }
                        };
                    }
                });

        var result = resolver.resolve(nestedMetadata, "mango-link");

        assertThat(result.version()).isEqualTo("2.0.0-nested");
        assertThat(result.source()).isEqualTo("JAR_MANIFEST");
    }

    @Test
    void unsupportedNestedConnectionSafelyDegradesToUnknown() throws Exception {
        URL unsupported = new URL(null, "jar:nested:/broken.jar!/META-INF/mango/module.properties",
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) {
                        throw new SecurityException("denied");
                    }
                });

        var result = resolver.resolve(unsupported, "mango-link");

        assertThat(result.version()).isNull();
        assertThat(result.source()).isEqualTo("UNKNOWN");
    }

    private Manifest manifest(String version) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, version);
        return manifest;
    }

    private byte[] manifestBytes(String version) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        manifest(version).write(output);
        return output.toByteArray();
    }

    private void writeEntry(JarOutputStream output, String name, String content) throws IOException {
        output.putNextEntry(new JarEntry(name));
        output.write(content.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        output.closeEntry();
    }
}
