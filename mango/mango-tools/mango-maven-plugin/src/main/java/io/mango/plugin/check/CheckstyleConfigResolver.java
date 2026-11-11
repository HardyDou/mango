package io.mango.plugin.check;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return resolve(rootPath, mangoConfigLocation, standardConfigLocation, rulesetDir, null, null);
    }

    ResolvedConfig resolve(
            Path rootPath,
            String mangoConfigLocation,
            String standardConfigLocation,
            String rulesetDir,
            String cyclomaticComplexityMax,
            String fileLengthMax)
            throws MojoExecutionException {
        ResolvedConfig resolved = resolveBase(
                rootPath, mangoConfigLocation, standardConfigLocation, rulesetDir);
        return applyOverrides(rootPath, resolved, cyclomaticComplexityMax, fileLengthMax);
    }

    private ResolvedConfig resolveBase(
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

    private ResolvedConfig applyOverrides(
            Path rootPath,
            ResolvedConfig resolved,
            String cyclomaticComplexityMax,
            String fileLengthMax)
            throws MojoExecutionException {
        boolean overrideCyclomatic = normalized(cyclomaticComplexityMax) != null;
        boolean overrideFileLength = normalized(fileLengthMax) != null;
        if (!overrideCyclomatic && !overrideFileLength) {
            return resolved;
        }
        Path source = Path.of(resolved.location());
        String content;
        try {
            content = Files.readString(source, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "Failed to read Checkstyle configuration for threshold override: " + source,
                    exception);
        }
        String updated = content;
        if (overrideCyclomatic) {
            updated = overrideModuleMax(
                    updated,
                    "CyclomaticComplexity",
                    positiveInt(cyclomaticComplexityMax, "mango.check.checkstyle.cyclomaticComplexityMax"),
                    "mango.check.checkstyle.cyclomaticComplexityMax");
        }
        if (overrideFileLength) {
            updated = overrideModuleMax(
                    updated,
                    "FileLength",
                    positiveInt(fileLengthMax, "mango.check.checkstyle.fileLengthMax"),
                    "mango.check.checkstyle.fileLengthMax");
        }
        Path output = rootPath.resolve(BundledRulesetResolver.OUTPUT_DIRECTORY)
                .resolve("checkstyle-resolved.xml")
                .normalize();
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, updated, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "Failed to write Checkstyle configuration with threshold overrides: " + output,
                    exception);
        }
        return new ResolvedConfig(output.toAbsolutePath().toString(), resolved.source() + ":override");
    }

    private String overrideModuleMax(String content, String module, int max, String property)
            throws MojoExecutionException {
        Pattern modulePattern = Pattern.compile(
                "(<module\\s+name=\"" + Pattern.quote(module) + "\"[^>]*>)([\\s\\S]*?)(</module>)");
        Matcher moduleMatcher = modulePattern.matcher(content);
        if (!moduleMatcher.find()) {
            throw new MojoExecutionException(
                    property + " requires Checkstyle module " + module + " in the selected ruleset");
        }
        String moduleBody = moduleMatcher.group(2);
        Pattern maxPattern = Pattern.compile("(<property\\s+name=\"max\"\\s+value=\")(\\d+)(\"/>)");
        Matcher maxMatcher = maxPattern.matcher(moduleBody);
        if (!maxMatcher.find()) {
            throw new MojoExecutionException(
                    property + " requires a max property in Checkstyle module " + module);
        }
        String updatedBody = moduleBody.substring(0, maxMatcher.start())
                + maxMatcher.group(1)
                + max
                + maxMatcher.group(3)
                + moduleBody.substring(maxMatcher.end());
        return content.substring(0, moduleMatcher.start())
                + moduleMatcher.group(1)
                + updatedBody
                + moduleMatcher.group(3)
                + content.substring(moduleMatcher.end());
    }

    private int positiveInt(String value, String property) throws MojoExecutionException {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // fall through to the shared error below
        }
        throw new MojoExecutionException(property + " must be a positive integer: " + value);
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    record ResolvedConfig(String location, String source) {}
}
