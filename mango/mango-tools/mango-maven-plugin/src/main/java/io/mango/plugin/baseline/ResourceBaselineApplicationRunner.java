package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Runs the final application against a disposable migration replay database. */
final class ResourceBaselineApplicationRunner {

    private static final String BASELINE_ENVIRONMENT = "mango-resource-baseline-build";
    private static final long TERMINATION_TIMEOUT_SECONDS = 5L;

    private final ResourceBaselineExecutionSettings settings;
    private final BaselineGenerationSettings generationSettings;
    private final MySqlBaselineStore store;
    private final Log log;

    ResourceBaselineApplicationRunner(
            ResourceBaselineExecutionSettings settings,
            BaselineGenerationSettings generationSettings,
            MySqlBaselineStore store,
            Log log) {
        this.settings = settings;
        this.generationSettings = generationSettings;
        this.store = store;
        this.log = log;
    }

    void materialize(String database) throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-cp");
        command.add(String.join(System.getProperty("path.separator"), settings.runtimeClasspath()));
        command.add(settings.applicationClass());
        command.add("bootstrap");
        command.add("apply");
        command.add("--mango.bootstrap.strategy=cold");
        command.add("--mango.bootstrap.environment-key=" + BASELINE_ENVIRONMENT);
        command.add("--mango.release.id=resource-baseline-build");
        command.add("--mango.release.revision=portable");
        command.add("--mango.release.generation=1");
        command.add("--mango.persistence.flyway.cold-baseline.enabled=false");
        command.add("--mango.bootstrap.resource-baseline-build-enabled=true");
        command.add("--mango.resource.registry.baseline-build-enabled=true");
        command.add("--mango.resource.registry.demo-enabled=false");
        command.add("--spring.main.banner-mode=off");

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(settings.workingDirectory().toFile())
                .inheritIO();
        Map<String, String> environment = builder.environment();
        environment.put("SPRING_DATASOURCE_URL", store.databaseUrl(database));
        environment.put("SPRING_DATASOURCE_USERNAME", generationSettings.username());
        environment.put("SPRING_DATASOURCE_PASSWORD", generationSettings.password());
        Process process = null;
        try {
            process = builder.start();
            log.info("Resource baseline application started: database=" + database);
            boolean completed = process.waitFor(settings.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                terminate(process);
                throw new MojoExecutionException(
                        "MANGO-BASELINE-044 Resource baseline application timed out; database=" + database);
            }
            if (process.exitValue() != 0) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-045 Resource baseline application failed; database=" + database
                                + ", exitCode=" + process.exitValue());
            }
            store.preparePortableResourceBaseline(database);
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-046 failed to start Resource baseline application", exception);
        } catch (InterruptedException exception) {
            terminate(process);
            Thread.currentThread().interrupt();
            throw new MojoExecutionException(
                    "MANGO-BASELINE-047 interrupted while waiting for Resource baseline application", exception);
        }
    }

    private static void terminate(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}
