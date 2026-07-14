package io.mango.plugin.architecture;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.graph.Dependency;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Resolves the effective Maven dependency bytecode for direct Reactor subjects. */
final class InternalDependencyClasspathResolver {

    private static final String TEST_SCOPE = "test";
    private static final String IMPORT_SCOPE = "import";
    private final ProjectDependenciesResolver resolver;

    InternalDependencyClasspathResolver(ProjectDependenciesResolver resolver) {
        this.resolver = resolver;
    }

    Set<Path> resolve(
            MavenSession session,
            Collection<MavenProject> reactorProjects,
            Collection<Path> reactorClassDirectories,
            Collection<String> internalGroupPrefixes)
            throws MojoExecutionException {
        Set<Path> classpath = normalize(reactorClassDirectories);
        for (MavenProject project : reactorProjects) {
            DependencyResolutionResult result = resolveProject(session, project);
            failOnUnresolvedInternalDependencies(
                    project, result.getUnresolvedDependencies(), internalGroupPrefixes);
            for (Dependency dependency : result.getResolvedDependencies()) {
                if (isInternalClasspathDependency(dependency, internalGroupPrefixes)) {
                    classpath.add(requireArtifactFile(project, dependency));
                }
            }
        }
        return classpath;
    }

    private DependencyResolutionResult resolveProject(MavenSession session, MavenProject project)
            throws MojoExecutionException {
        try {
            return resolver.resolve(
                    new DefaultDependencyResolutionRequest(
                            project, session.getRepositorySession()));
        } catch (DependencyResolutionException exception) {
            throw new MojoExecutionException(
                    "MANGO-ARCH-ENGINE-010 unable to resolve effective dependencies for "
                            + project.getArtifactId(),
                    exception);
        }
    }

    private void failOnUnresolvedInternalDependencies(
            MavenProject project,
            Collection<Dependency> unresolvedDependencies,
            Collection<String> internalGroupPrefixes)
            throws MojoExecutionException {
        for (Dependency dependency : unresolvedDependencies) {
            if (isInternalClasspathDependency(dependency, internalGroupPrefixes)) {
                throw new MojoExecutionException(
                        "MANGO-ARCH-ENGINE-010 unresolved internal dependency for "
                                + project.getArtifactId()
                                + ": "
                                + dependency.getArtifact());
            }
        }
    }

    private boolean isInternalClasspathDependency(
            Dependency dependency, Collection<String> internalGroupPrefixes) {
        if (TEST_SCOPE.equals(dependency.getScope())
                || IMPORT_SCOPE.equals(dependency.getScope())) {
            return false;
        }
        String groupId = dependency.getArtifact().getGroupId();
        return internalGroupPrefixes.stream()
                .anyMatch(prefix -> groupId.equals(prefix) || groupId.startsWith(prefix + "."));
    }

    private Path requireArtifactFile(MavenProject project, Dependency dependency)
            throws MojoExecutionException {
        if (dependency.getArtifact().getFile() == null) {
            throw new MojoExecutionException(
                    "MANGO-ARCH-ENGINE-010 internal dependency has no resolved file for "
                            + project.getArtifactId()
                            + ": "
                            + dependency.getArtifact());
        }
        Path artifact = dependency.getArtifact().getFile().toPath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(artifact) && !Files.isDirectory(artifact)) {
            throw new MojoExecutionException(
                    "MANGO-ARCH-ENGINE-010 missing resolved internal dependency bytecode: "
                            + artifact);
        }
        return artifact;
    }

    private Set<Path> normalize(Collection<Path> paths) {
        Set<Path> normalized = new LinkedHashSet<>();
        paths.stream().map(path -> path.toAbsolutePath().normalize()).forEach(normalized::add);
        return normalized;
    }
}
