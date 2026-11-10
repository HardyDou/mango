package io.mango.plugin.check;

import org.apache.maven.plugin.MojoExecutionException;

import java.nio.file.Files;
import java.nio.file.Path;

final class CheckstyleConfigResolver {

    static final String BUNDLED_CONFIG_RESOURCE = BundledRulesetResolver.CHECKSTYLE_RESOURCE;
    static final String PROJECT_CONFIG_PATH = "config/quality/checkstyle.xml";

    private final ClassLoader classLoader;

    CheckstyleConfigResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    ResolvedConfig resolve(
            Path rootPath, String mangoConfigLocation, String standardConfigLocation, String rulesetDir)
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

        String fromRulesetDir = new BundledRulesetResolver(classLoader)
                .resolveFromDirectory(rootPath, rulesetDir, "checkstyle.xml");
        if (fromRulesetDir != null) {
            return new ResolvedConfig(fromRulesetDir, "rulesetDir:checkstyle.xml");
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
        String location = new BundledRulesetResolver(classLoader)
                .extract(rootPath, BUNDLED_CONFIG_RESOURCE, "checkstyle.xml");
        return new ResolvedConfig(location, "default:mango-bundled");
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    record ResolvedConfig(String location, String source) {}
}
