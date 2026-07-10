package io.mango.plugin.check;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs the executable PMO quality contract from Maven.
 *
 * <p>The Java plugin deliberately delegates policy evaluation to the versioned
 * PMO Node tool. This keeps Maven, the package distributed to business
 * projects, and CI on one policy implementation instead of duplicating rules.</p>
 */
@Mojo(name = "quality-gate", defaultPhase = LifecyclePhase.VERIFY, aggregator = true, threadSafe = true)
public class QualityGateMojo extends AbstractMojo {

    private static final long DEFAULT_TIMEOUT_SECONDS = 600;
    private static final List<String> TOOL_LOCATIONS = List.of(
            "mango-pmo/tools/quality-gate.mjs",
            "business-pmo/mango-baseline/tools/quality-gate.mjs");

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private String baseDir;

    @Parameter(property = "mango.quality.node", defaultValue = "node")
    private String nodeExecutable;

    @Parameter(property = "mango.quality.repositoryRoot")
    private String repositoryRoot;

    @Parameter(property = "mango.quality.tool")
    private String toolFile;

    @Parameter(property = "mango.quality.baseRef", defaultValue = "origin/main")
    private String baseRef;

    @Parameter(property = "mango.quality.headRef", defaultValue = "HEAD")
    private String headRef;

    @Parameter(property = "mango.quality.contract")
    private String contractFile;

    @Parameter(property = "mango.quality.report", defaultValue = ".runtime/pmo/maven-quality-gate.json")
    private String reportFile;

    @Parameter(property = "mango.quality.timeoutSeconds", defaultValue = "600")
    private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    @Override
    public void execute() throws MojoExecutionException {
        Path executionRoot = resolveExecutionRoot();
        Path gateTool = resolveTool(executionRoot);
        Path root = resolveRepositoryRoot(executionRoot, gateTool);
        List<String> command = buildCommand(root, gateTool);
        getLog().info("Running executable PMO quality gate from " + root);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(root.toFile());
        builder.redirectErrorStream(true);
        builder.inheritIO();
        try {
            Process process = builder.start();
            boolean completed = process.waitFor(Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new MojoExecutionException("Executable PMO quality gate timed out after "
                        + timeoutSeconds + " seconds");
            }
            if (process.exitValue() != 0) {
                throw new MojoExecutionException("Executable PMO quality gate failed with exit code "
                        + process.exitValue());
            }
        } catch (IOException exception) {
            throw new MojoExecutionException("Cannot start executable PMO quality gate: "
                    + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Executable PMO quality gate was interrupted", exception);
        }
    }

    private Path resolveExecutionRoot() {
        if (session != null && session.getExecutionRootDirectory() != null) {
            return Paths.get(session.getExecutionRootDirectory()).toAbsolutePath().normalize();
        }
        if (baseDir != null && !baseDir.isBlank()) {
            return Paths.get(baseDir).toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private Path resolveTool(Path executionRoot) throws MojoExecutionException {
        if (toolFile != null && !toolFile.isBlank()) {
            Path explicit = resolveAgainst(executionRoot, toolFile);
            if (!Files.isRegularFile(explicit)) {
                throw new MojoExecutionException("PMO quality gate tool does not exist: " + explicit);
            }
            return explicit;
        }

        for (Path candidateRoot : candidateRoots(executionRoot)) {
            for (String location : TOOL_LOCATIONS) {
                Path candidate = candidateRoot.resolve(location).normalize();
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        throw new MojoExecutionException("Cannot locate PMO quality gate tool from " + executionRoot
                + "; set -Dmango.quality.tool=<path> explicitly");
    }

    private Path resolveRepositoryRoot(Path executionRoot, Path gateTool) {
        if (repositoryRoot != null && !repositoryRoot.isBlank()) {
            return resolveAgainst(executionRoot, repositoryRoot);
        }
        Path current = gateTool.getParent();
        while (current != null) {
            if (Files.exists(current.resolve(".git")) || Files.isDirectory(current.resolve("mango-pmo"))
                    || Files.isDirectory(current.resolve("business-pmo"))) {
                return current;
            }
            current = current.getParent();
        }
        return executionRoot;
    }

    private List<Path> candidateRoots(Path executionRoot) {
        List<Path> roots = new ArrayList<>();
        Path current = executionRoot;
        while (current != null) {
            roots.add(current);
            current = current.getParent();
        }
        return roots;
    }

    private List<String> buildCommand(Path root, Path gateTool) {
        List<String> command = new ArrayList<>();
        command.add(nodeExecutable);
        command.add(gateTool.toString());
        command.add("--root");
        command.add(root.toString());
        command.add("--base");
        command.add(baseRef);
        command.add("--head");
        command.add(headRef);
        command.add("--report");
        command.add(resolveAgainst(root, reportFile).toString());
        if (contractFile != null && !contractFile.isBlank()) {
            command.add("--contract");
            command.add(resolveAgainst(root, contractFile).toString());
        }
        return command;
    }

    private Path resolveAgainst(Path root, String value) {
        Path path = Paths.get(value);
        return (path.isAbsolute() ? path : root.resolve(path)).toAbsolutePath().normalize();
    }
}
