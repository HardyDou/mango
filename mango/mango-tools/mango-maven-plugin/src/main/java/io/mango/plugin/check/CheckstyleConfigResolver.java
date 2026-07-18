package io.mango.plugin.check;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class CheckstyleConfigResolver {

    static final String BUNDLED_CONFIG_RESOURCE = "rulesets/java/checkstyle.xml";
    static final String PROJECT_CONFIG_PATH = "config/quality/checkstyle.xml";

    private final ClassLoader classLoader;

    CheckstyleConfigResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    ResolvedConfig resolve(
            Path rootPath, String mangoConfigLocation, String standardConfigLocation)
            throws MojoExecutionException {
        String explicitMangoConfig = normalized(mangoConfigLocation);
        if (explicitMangoConfig != null) {
            return new ResolvedConfig(
                    resolveExistingFile(rootPath, explicitMangoConfig),
                    "custom:mango.check.checkstyleConfigLocation");
        }

        String explicitStandardConfig = normalized(standardConfigLocation);
        if (explicitStandardConfig != null) {
            return new ResolvedConfig(
                    resolveExistingFile(rootPath, explicitStandardConfig),
                    "custom:checkstyle.config.location");
        }

        Path projectConfig = rootPath.resolve(PROJECT_CONFIG_PATH).normalize();
        if (Files.isRegularFile(projectConfig)) {
            return new ResolvedConfig(
                    projectConfig.toAbsolutePath().toString(), "project:" + PROJECT_CONFIG_PATH);
        }

        return extractBundledConfig(rootPath);
    }

    private String resolveExistingFile(Path rootPath, String configuredLocation) {
        Path configuredPath = Path.of(configuredLocation);
        Path resolvedPath = configuredPath.normalize();
        if (!configuredPath.isAbsolute()) {
            resolvedPath = rootPath.resolve(configuredPath).normalize();
        }
        if (Files.isRegularFile(resolvedPath)) {
            return resolvedPath.toAbsolutePath().toString();
        }
        return configuredLocation;
    }

    private ResolvedConfig extractBundledConfig(Path rootPath) throws MojoExecutionException {
        Path output = rootPath.resolve("target/mango-check/checkstyle.xml").normalize();
        try (InputStream input = classLoader.getResourceAsStream(BUNDLED_CONFIG_RESOURCE)) {
            if (input == null) {
                throw new MojoExecutionException(
                        "Bundled Mango Checkstyle configuration is missing: "
                                + BUNDLED_CONFIG_RESOURCE);
            }
            Files.createDirectories(output.getParent());
            Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
            return new ResolvedConfig(
                    output.toAbsolutePath().toString(), "default:mango-bundled");
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "Failed to prepare bundled Mango Checkstyle configuration", exception);
        }
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    record ResolvedConfig(String location, String source) {}
}
