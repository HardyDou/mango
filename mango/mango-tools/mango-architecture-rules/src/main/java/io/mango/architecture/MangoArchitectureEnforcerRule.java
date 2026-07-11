package io.mango.architecture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/** Maven Enforcer entry point for Mango module dependency rules. */
@Named("mangoArchitecture")
public final class MangoArchitectureEnforcerRule extends AbstractEnforcerRule {

    private final MavenSession session;

    @Inject
    public MangoArchitectureEnforcerRule(MavenSession session) {
        this.session = session;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        try {
            List<MavenProject> projects = session.getProjects();
            if (projects == null || projects.isEmpty()) {
                MavenProject currentProject = session.getCurrentProject();
                projects = currentProject == null ? List.of() : List.of(currentProject);
            }
            if (projects.isEmpty()) {
                throw new EnforcerRuleException(
                        "MANGO-ARCH-ENGINE-001 MavenSession has no Reactor projects");
            }
            Set<String> reactorArtifacts = projects.stream()
                    .map(MavenProject::getArtifactId)
                    .collect(Collectors.toSet());
            Set<String> changedPomPaths = changedPomPaths();
            MavenDependencyChecker checker = new MavenDependencyChecker();
            List<ArchitectureIssue> issues = projects.stream()
                    .filter(project -> changedPomPaths.contains(relativePomPath(project)))
                    .flatMap(project -> checker.check(
                            project.getArtifactId(), project.getDependencies(), reactorArtifacts).stream())
                    .toList();
            if (!issues.isEmpty()) {
                String message = issues.stream()
                        .map(issue -> issue.ruleId() + " " + issue.subject() + ": " + issue.message())
                        .collect(Collectors.joining(System.lineSeparator()));
                throw new EnforcerRuleException(message);
            }
        } catch (EnforcerRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EnforcerRuleException("MANGO-ARCH-ENGINE-001 unable to inspect MavenProject", exception);
        }
    }

    private Set<String> changedPomPaths() throws EnforcerRuleException {
        String configuredBase = session.getUserProperties().getProperty("mango.architecture.base");
        String base = configuredBase == null || configuredBase.isBlank()
                ? resolveDefaultBase()
                : configuredBase;
        Set<String> paths = new LinkedHashSet<>(runGit("diff", "--name-only", "--diff-filter=ACMR", base));
        paths.addAll(runGit("ls-files", "--others", "--exclude-standard"));
        return paths.stream().filter(path -> path.endsWith("pom.xml")).collect(Collectors.toSet());
    }

    private String resolveDefaultBase() throws EnforcerRuleException {
        for (String candidate : List.of("main", "origin/main")) {
            try {
                List<String> result = runGit("merge-base", "HEAD", candidate);
                if (!result.isEmpty()) {
                    return result.get(0);
                }
            } catch (EnforcerRuleException exception) {
                getLog().debug("Git base candidate unavailable: " + candidate);
            }
        }
        List<String> parent = runGit("rev-parse", "HEAD^");
        if (parent.isEmpty()) {
            throw new EnforcerRuleException("MANGO-ARCH-ENGINE-008 unable to resolve Git base");
        }
        return parent.get(0);
    }

    private List<String> runGit(String... arguments) throws EnforcerRuleException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(Path.of(session.getExecutionRootDirectory()).toFile())
                .redirectErrorStream(true);
        try {
            Process process = builder.start();
            List<String> output;
            try (var reader = process.inputReader()) {
                output = reader.lines().toList();
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new EnforcerRuleException(
                        "MANGO-ARCH-ENGINE-008 git command failed (" + exitCode + "): "
                                + String.join(" ", command) + System.lineSeparator()
                                + String.join(System.lineSeparator(), output));
            }
            return output;
        } catch (IOException exception) {
            throw new EnforcerRuleException("MANGO-ARCH-ENGINE-008 unable to execute Git", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EnforcerRuleException("MANGO-ARCH-ENGINE-008 Git command interrupted", exception);
        }
    }

    private String relativePomPath(MavenProject project) {
        Path root = Path.of(session.getExecutionRootDirectory()).toAbsolutePath().normalize();
        return root.relativize(project.getFile().toPath().toAbsolutePath().normalize())
                .toString().replace('\\', '/');
    }

}
