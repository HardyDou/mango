package io.mango.architecture;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

/** Maven Enforcer entry point for Mango module dependency rules. */
@Named("mangoArchitecture")
public final class MangoArchitectureEnforcerRule extends AbstractEnforcerRule {

    private static final String GROUP_SEPARATOR = ",";
    private static final List<String> DEFAULT_BASES = List.of("main", "origin/main");

    private final MavenSession session;

    @Inject
    public MangoArchitectureEnforcerRule(MavenSession session) {
        this.session = session;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        try {
            List<MavenProject> projects = reactorProjects();
            Set<String> reactorArtifacts =
                    projects.stream().map(MavenProject::getArtifactId).collect(Collectors.toSet());
            Set<String> governedGroupPrefixes = governedGroupPrefixes(projects);
            List<ArchitectureIssue> issues =
                    dependencyIssues(projects, reactorArtifacts, governedGroupPrefixes);
            if (!issues.isEmpty()) {
                String message =
                        issues.stream()
                                .map(
                                        issue ->
                                                issue.ruleId()
                                                        + " "
                                                        + issue.subject()
                                                        + ": "
                                                        + issue.message())
                                .collect(Collectors.joining(System.lineSeparator()));
                throw new EnforcerRuleException(message);
            }
        } catch (EnforcerRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EnforcerRuleException(
                    "MANGO-ARCH-ENGINE-001 unable to inspect MavenProject", exception);
        }
    }

    private List<MavenProject> reactorProjects() throws EnforcerRuleException {
        List<MavenProject> projects = session.getProjects();
        if (projects == null || projects.isEmpty()) {
            MavenProject currentProject = session.getCurrentProject();
            if (currentProject == null) {
                projects = List.of();
            } else {
                projects = List.of(currentProject);
            }
        }
        if (projects.isEmpty()) {
            throw new EnforcerRuleException(
                    "MANGO-ARCH-ENGINE-001 MavenSession has no Reactor projects");
        }
        return projects;
    }

    private Set<String> governedGroupPrefixes(List<MavenProject> projects) {
        Set<String> governedGroupPrefixes =
                projects.stream()
                        .map(MavenProject::getGroupId)
                        .filter(groupId -> groupId != null && !groupId.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        String configuredGroups =
                session.getUserProperties()
                        .getProperty("mango.architecture.businessGroupPrefixes", "");
        for (String configuredGroup : configuredGroups.split(GROUP_SEPARATOR)) {
            if (!configuredGroup.isBlank()) {
                governedGroupPrefixes.add(configuredGroup.trim());
            }
        }
        return governedGroupPrefixes;
    }

    private List<ArchitectureIssue> dependencyIssues(
            List<MavenProject> projects,
            Set<String> reactorArtifacts,
            Set<String> governedGroupPrefixes)
            throws EnforcerRuleException {
        Set<String> changedPomPaths = changedPomPaths();
        MavenDependencyChecker checker = new MavenDependencyChecker();
        return projects.stream()
                .filter(project -> changedPomPaths.contains(relativePomPath(project)))
                .flatMap(
                        project ->
                                checker
                                        .check(
                                                project.getArtifactId(),
                                                project.getDependencies(),
                                                reactorArtifacts,
                                                governedGroupPrefixes)
                                        .stream())
                .toList();
    }

    private Set<String> changedPomPaths() throws EnforcerRuleException {
        String configuredBase = session.getUserProperties().getProperty("mango.architecture.base");
        String base = configuredBase;
        if (base == null || base.isBlank()) {
            base = resolveDefaultBase();
        }
        Set<String> paths =
                new LinkedHashSet<>(
                        runGit("diff", "--name-only", "--relative", "--diff-filter=ACMR", base));
        paths.addAll(runGit("ls-files", "--others", "--exclude-standard"));
        return paths.stream().filter(path -> path.endsWith("pom.xml")).collect(Collectors.toSet());
    }

    private String resolveDefaultBase() throws EnforcerRuleException {
        for (String candidate : DEFAULT_BASES) {
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
        ProcessBuilder builder =
                new ProcessBuilder(command)
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
                        "MANGO-ARCH-ENGINE-008 git command failed ("
                                + exitCode
                                + "): "
                                + String.join(" ", command)
                                + System.lineSeparator()
                                + String.join(System.lineSeparator(), output));
            }
            return output;
        } catch (IOException exception) {
            throw new EnforcerRuleException(
                    "MANGO-ARCH-ENGINE-008 unable to execute Git", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EnforcerRuleException(
                    "MANGO-ARCH-ENGINE-008 Git command interrupted", exception);
        }
    }

    private String relativePomPath(MavenProject project) {
        Path root = Path.of(session.getExecutionRootDirectory()).toAbsolutePath().normalize();
        return root.relativize(project.getFile().toPath().toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');
    }
}
