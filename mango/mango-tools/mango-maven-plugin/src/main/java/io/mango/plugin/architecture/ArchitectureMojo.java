package io.mango.plugin.architecture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.mango.architecture.ArchitectureIssue;
import io.mango.architecture.MangoArchUnitChecker;
import io.mango.architecture.MangoPmdChecker;
import io.mango.architecture.MavenDependencyChecker;
import io.mango.architecture.ModuleRole;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/** Aggregates all local architecture engines into one fail-closed Maven verify goal. */
@Mojo(
        name = "architecture",
        defaultPhase = LifecyclePhase.VERIFY,
        aggregator = true,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class ArchitectureMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(
            defaultValue = "${maven.multiModuleProjectDirectory}/target/mango-architecture-report.json")
    private Path reportFile;

    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}", readonly = true, required = true)
    private Path rootDirectory;

    @Parameter(property = "mango.architecture.base")
    private String gitBase;

    @Parameter(property = "mango.architecture.mode", defaultValue = "changed")
    private String mode;

    @Parameter
    private List<String> excludedModules = List.of();

    @Parameter
    private List<String> allowedReverseControllers = List.of();

    @Parameter(property = "mango.architecture.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }
        long startedAt = System.nanoTime();
        List<ArchitectureIssue> dependencyIssues = new ArrayList<>();
        Map<Path, ModuleRole> classDirectories = new LinkedHashMap<>();
        List<Path> sourceDirectories = new ArrayList<>();
        Set<String> reactorArtifactIds = session.getProjects().stream()
                .map(MavenProject::getArtifactId).collect(Collectors.toSet());
        for (MavenProject reactorProject : session.getProjects()) {
            dependencyIssues.addAll(new MavenDependencyChecker().check(
                    reactorProject.getArtifactId(), reactorProject.getDependencies(), reactorArtifactIds));
            collectJavaInputs(reactorProject, classDirectories, sourceDirectories);
        }
        if (sourceDirectories.isEmpty()) {
            throw new MojoExecutionException(
                    "MANGO-ARCH-ENGINE-005 Reactor contains no Java source directories");
        }
        List<ArchitectureIssue> bytecodeIssues = new MangoArchUnitChecker(
                Set.copyOf(allowedReverseControllers)).check(classDirectories);
        String javaVersion = project.getProperties().getProperty(
                "maven.compiler.release",
                project.getProperties().getProperty("maven.compiler.source", "17"));
        List<ArchitectureIssue> sourceIssues = new MangoPmdChecker().check(
                sourceDirectories, javaVersion, classDirectories.keySet());
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
        List<ArchitectureIssue> allIssues = new ArrayList<>();
        allIssues.addAll(dependencyIssues);
        allIssues.addAll(bytecodeIssues);
        allIssues.addAll(sourceIssues);
        List<ArchitectureIssue> blockingIssues = "full".equalsIgnoreCase(mode)
                ? List.copyOf(allIssues)
                : changedIssues(allIssues);
        ArchitectureReport report = new ArchitectureReport(
                dependencyIssues, bytecodeIssues, sourceIssues, blockingIssues,
                mode, durationMillis);
        writeReport(report);
        int issueCount = report.issueCount();
        getLog().info("Mango architecture: dependency=" + dependencyIssues.size()
                + ", archunit=" + bytecodeIssues.size() + ", pmd=" + sourceIssues.size()
                + ", blocking=" + blockingIssues.size() + ", mode=" + mode
                + ", durationMs=" + durationMillis);
        if (issueCount > 0) {
            throw new MojoFailureException(
                    "Mango architecture gate found " + issueCount + " violation(s); report: " + reportFile);
        }
    }

    private List<ArchitectureIssue> changedIssues(List<ArchitectureIssue> allIssues)
            throws MojoExecutionException {
        Set<String> changedPaths = gitChangedPaths();
        if (changedPaths.isEmpty()) {
            return List.of();
        }
        Set<String> changedArtifacts = session.getProjects().stream()
                .filter(reactorProject -> changedPaths.contains(relativePath(reactorProject.getFile().toPath())))
                .map(MavenProject::getArtifactId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> changedClasses = changedPaths.stream()
                .filter(path -> path.endsWith(".java") && path.contains("/src/main/java/"))
                .map(path -> path.substring(path.indexOf("/src/main/java/") + 15, path.length() - 5)
                        .replace('/', '.'))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return allIssues.stream()
                .filter(issue -> isChangedIssue(issue, changedPaths, changedArtifacts, changedClasses))
                .toList();
    }

    private boolean isChangedIssue(
            ArchitectureIssue issue,
            Set<String> changedPaths,
            Set<String> changedArtifacts,
            Set<String> changedClasses) {
        String subject = issue.subject().replace('\\', '/');
        if (changedPaths.stream().anyMatch(path -> subject.startsWith(rootDirectory.resolve(path)
                .toAbsolutePath().normalize().toString().replace('\\', '/')))) {
            return true;
        }
        if (changedArtifacts.stream().anyMatch(artifact -> subject.startsWith(artifact + " ->"))) {
            return true;
        }
        return changedClasses.stream().anyMatch(className -> subject.equals(className)
                || subject.startsWith(className + "$") || subject.startsWith(className + "."));
    }

    private Set<String> gitChangedPaths() throws MojoExecutionException {
        String base = gitBase == null || gitBase.isBlank() ? resolveDefaultBase() : gitBase;
        Set<String> paths = new LinkedHashSet<>(runGit("diff", "--name-only", "--diff-filter=ACMR", base));
        paths.addAll(runGit("ls-files", "--others", "--exclude-standard"));
        return paths.stream().filter(path -> !path.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolveDefaultBase() throws MojoExecutionException {
        for (String candidate : List.of("main", "origin/main")) {
            try {
                List<String> result = runGit("merge-base", "HEAD", candidate);
                if (!result.isEmpty()) {
                    return result.get(0);
                }
            } catch (MojoExecutionException ignored) {
                getLog().debug("Git base candidate unavailable: " + candidate);
            }
        }
        List<String> parent = runGit("rev-parse", "HEAD^");
        if (parent.isEmpty()) {
            throw new MojoExecutionException("MANGO-ARCH-ENGINE-008 unable to resolve Git base");
        }
        return parent.get(0);
    }

    private List<String> runGit(String... arguments) throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(rootDirectory.toAbsolutePath().normalize().toFile())
                .redirectErrorStream(true);
        try {
            Process process = builder.start();
            List<String> output;
            try (var reader = process.inputReader()) {
                output = reader.lines().toList();
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new MojoExecutionException(
                        "MANGO-ARCH-ENGINE-008 git command failed (" + exitCode + "): "
                                + String.join(" ", command) + "\n" + String.join("\n", output));
            }
            return output;
        } catch (IOException exception) {
            throw new MojoExecutionException("MANGO-ARCH-ENGINE-008 unable to execute Git", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("MANGO-ARCH-ENGINE-008 Git command interrupted", exception);
        }
    }

    private String relativePath(Path path) {
        return rootDirectory.toAbsolutePath().normalize()
                .relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private void collectJavaInputs(
            MavenProject reactorProject,
            Map<Path, ModuleRole> classDirectories,
            List<Path> sourceDirectories) throws MojoExecutionException {
        Path sourceDirectory = Path.of(reactorProject.getBuild().getSourceDirectory())
                .toAbsolutePath().normalize();
        if (excludedModules.contains(reactorProject.getArtifactId())) {
            return;
        }
        if (!containsJava(sourceDirectory)) {
            return;
        }
        Path classDirectory = Path.of(reactorProject.getBuild().getOutputDirectory())
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(classDirectory)) {
            throw new MojoExecutionException(
                    "MANGO-ARCH-ENGINE-003 missing compiled classes for "
                            + reactorProject.getArtifactId() + ": " + classDirectory);
        }
        sourceDirectories.add(sourceDirectory);
        classDirectories.put(classDirectory, ModuleRole.fromArtifactId(reactorProject.getArtifactId()));
    }

    private boolean containsJava(Path sourceDirectory) throws MojoExecutionException {
        if (!Files.isDirectory(sourceDirectory)) {
            return false;
        }
        try (var files = Files.walk(sourceDirectory)) {
            return files.anyMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().endsWith(".java"));
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "MANGO-ARCH-ENGINE-005 unable to inspect source directory " + sourceDirectory,
                    exception);
        }
    }

    private void writeReport(ArchitectureReport report) throws MojoExecutionException {
        try {
            Files.createDirectories(reportFile.toAbsolutePath().normalize().getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValue(reportFile.toFile(), report);
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "MANGO-ARCH-ENGINE-007 unable to write report " + reportFile, exception);
        }
    }

    public record ArchitectureReport(
            List<ArchitectureIssue> dependencyIssues,
            List<ArchitectureIssue> archUnitIssues,
            List<ArchitectureIssue> pmdIssues,
            List<ArchitectureIssue> blockingIssues,
            String mode,
            long durationMillis) {

        public int issueCount() {
            return blockingIssues.size();
        }
    }
}
