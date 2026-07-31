package io.mango.plugin.baseline;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Replays final module Flyway migrations on temporary MySQL schemas, generates one verified cold
 * baseline per module, and registers the output as a Maven resource.
 */
@Mojo(
        name = "baseline-generate",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        threadSafe = false)
public final class BaselineGenerateMojo extends AbstractMojo {

    private static final String LIST_DELIMITER = ",";
    private static final String SCHEMA_PREFIX_PATTERN = "[a-z][a-z0-9_]{0,31}";
    private static final String DATASOURCE_GROUP_PATTERN = "[a-z][a-z0-9-]{0,31}";
    private static final String MODULE_PATTERN = "[a-z0-9][a-z0-9-]*";

    @Parameter(property = "mango.baseline.jdbcUrl", required = true)
    private String jdbcUrl;

    @Parameter(property = "mango.baseline.username", defaultValue = "root")
    private String username;

    @Parameter(property = "mango.baseline.passwordEnv", defaultValue = "MANGO_BASELINE_DB_PASSWORD")
    private String passwordEnv;

    @Parameter(property = "mango.baseline.allowEmptyPassword", defaultValue = "false")
    private boolean allowEmptyPassword;

    @Parameter(property = "mango.baseline.includedModules")
    private String includedModules;

    @Parameter(property = "mango.baseline.moduleOrder")
    private String moduleOrder;

    @Parameter(property = "mango.baseline.moduleGroups")
    private String moduleGroups;

    @Parameter(property = "mango.baseline.schemaPrefix", defaultValue = "mango_baseline")
    private String schemaPrefix;

    @Parameter(property = "mango.baseline.keepSchemas", defaultValue = "false")
    private boolean keepSchemas;

    @Parameter(
            property = "mango.baseline.searchDirectory",
            defaultValue = "${session.executionRootDirectory}",
            required = true)
    private File searchDirectory;

    @Parameter(
            property = "mango.baseline.outputDirectory",
            defaultValue = "${project.build.directory}/generated-resources",
            required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        validateConfiguration();
        String password = password();
        Set<String> includes = parseModuleSet(includedModules, "includedModules");
        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                searchDirectory.toPath(), project, includes);
        List<String> order = parseModuleList(moduleOrder, "moduleOrder");
        Map<String, String> groups = parseModuleGroups(moduleGroups);
        Path generatedResources = outputDirectory.toPath().toAbsolutePath().normalize();
        registerGeneratedResources(generatedResources);

        BaselineGenerationSettings settings = new BaselineGenerationSettings(
                jdbcUrl.trim(),
                username.trim(),
                password,
                schemaPrefix.trim(),
                generatedResources,
                order,
                groups,
                keepSchemas);
        new BaselineGenerator(settings, catalog, getLog()).generate();
    }

    private void validateConfiguration() throws MojoExecutionException {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-028 mango.baseline.jdbcUrl is required");
        }
        if (username == null || username.isBlank()) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-029 mango.baseline.username must not be blank");
        }
        if (schemaPrefix == null || !schemaPrefix.matches(SCHEMA_PREFIX_PATTERN)) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-030 mango.baseline.schemaPrefix must match [a-z][a-z0-9_]{0,31}");
        }
        if (searchDirectory == null || outputDirectory == null || project == null) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-031 Maven project, searchDirectory, and outputDirectory are required");
        }
    }

    private String password() throws MojoExecutionException {
        if (passwordEnv != null && !passwordEnv.isBlank()) {
            String value = System.getenv(passwordEnv.trim());
            if (value != null) {
                return value;
            }
            if (!allowEmptyPassword) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-032 password environment variable is not set: "
                                + passwordEnv.trim());
            }
        } else if (!allowEmptyPassword) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-033 configure mango.baseline.passwordEnv or explicitly allow an empty password");
        }
        return "";
    }

    private void registerGeneratedResources(Path directory) {
        String resourceDirectory = directory.toString();
        boolean registered = project.getResources().stream()
                .anyMatch(resource -> resourceDirectory.equals(resource.getDirectory()));
        if (!registered) {
            Resource resource = new Resource();
            resource.setDirectory(resourceDirectory);
            resource.setFiltering(false);
            project.addResource(resource);
        }
    }

    private static Set<String> parseModuleSet(String value, String property)
            throws MojoExecutionException {
        return new LinkedHashSet<>(parseModuleList(value, property));
    }

    private static List<String> parseModuleList(String value, String property)
            throws MojoExecutionException {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> modules = new ArrayList<>();
        for (String token : value.split(LIST_DELIMITER)) {
            String module = normalizeModule(token, property);
            modules.add(module);
        }
        return List.copyOf(modules);
    }

    private static Map<String, String> parseModuleGroups(String value)
            throws MojoExecutionException {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        Map<String, String> groups = new LinkedHashMap<>();
        for (String token : value.split(LIST_DELIMITER)) {
            String[] parts = token.trim().split("=", -1);
            if (parts.length != 2) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-034 moduleGroups entries must use module=group: " + token);
            }
            String module = normalizeModule(parts[0], "moduleGroups");
            String group = parts[1].trim().toLowerCase(Locale.ROOT);
            if (!group.matches(DATASOURCE_GROUP_PATTERN)) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-035 invalid datasource group: " + parts[1]);
            }
            String previous = groups.putIfAbsent(module, group);
            if (previous != null) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-036 duplicate moduleGroups entry: " + module);
            }
        }
        return Map.copyOf(groups);
    }

    private static String normalizeModule(String value, String property)
            throws MojoExecutionException {
        String module = value.trim().toLowerCase(Locale.ROOT);
        if (!module.matches(MODULE_PATTERN)) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-037 invalid module in " + property + ": " + value);
        }
        return module;
    }
}
