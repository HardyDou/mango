package io.mango.architecture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Dependency;

/** Enforcer-compatible dependency policy implemented from Maven's resolved model. */
public final class MavenDependencyChecker {

    private static final String FEIGN_INFRA = "mango-infra-feign-starter";
    private static final String RESOURCE_GROUP = "io.mango.platform.resource";
    private static final Set<String> RESOURCE_RUNTIME = Set.of(
            "mango-resource-core",
            "mango-resource-support",
            "mango-resource-starter",
            "mango-resource-sync-starter",
            "mango-resource-starter-remote");

    public List<ArchitectureIssue> check(String artifactId, Collection<Dependency> dependencies) {
        return check(artifactId, dependencies, Set.of());
    }

    public List<ArchitectureIssue> check(
            String artifactId,
            Collection<Dependency> dependencies,
            Set<String> reactorArtifactIds) {
        ModuleRole sourceRole = ModuleRole.fromArtifactId(artifactId);
        String sourceDomain = ModuleRole.domainOf(artifactId);
        List<ArchitectureIssue> issues = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            if (isTestOrProvided(dependency)) {
                continue;
            }
            String targetId = dependency.getArtifactId();
            if (sourceRole == ModuleRole.STARTER_REMOTE
                    && "spring-cloud-starter-openfeign".equals(targetId)) {
                issues.add(issue("MANGO-ARCH-DEP-004", artifactId, targetId,
                        "starter-remote must use mango-infra-feign-starter"));
                continue;
            }
            if (!isArchitecturalDependency(dependency, reactorArtifactIds)) {
                continue;
            }
            ModuleRole targetRole = ModuleRole.fromArtifactId(targetId);
            String targetDomain = ModuleRole.domainOf(targetId);
            if (sourceRole == ModuleRole.API
                    && (targetRole == ModuleRole.CORE || targetRole == ModuleRole.SUPPORT
                    || targetRole == ModuleRole.STARTER || targetRole == ModuleRole.STARTER_REMOTE)) {
                issues.add(issue("MANGO-ARCH-DEP-001", artifactId, targetId,
                        "api must not depend on core/support/starter"));
            }
            if (sourceRole == ModuleRole.CORE
                    && !sourceDomain.equals(targetDomain)
                    && (targetRole == ModuleRole.CORE || targetRole == ModuleRole.STARTER
                    || targetRole == ModuleRole.STARTER_REMOTE)) {
                issues.add(issue("MANGO-ARCH-DEP-002", artifactId, targetId,
                        "core must not depend on another domain core/starter"));
            }
            if (sourceRole == ModuleRole.SUPPORT
                    && (targetRole == ModuleRole.CORE || targetRole == ModuleRole.STARTER
                    || targetRole == ModuleRole.STARTER_REMOTE)) {
                issues.add(issue("MANGO-ARCH-DEP-006", artifactId, targetId,
                        "support must not depend on core/starter"));
            }
            if (isForbiddenResourceRuntimeDependency(artifactId, dependency)) {
                issues.add(issue("MANGO-ARCH-DEP-005", artifactId, targetId,
                        "non-resource library must depend on mango-resource-api, not resource runtime"));
            }
            if (sourceRole == ModuleRole.STARTER_REMOTE
                    && !isAllowedRemoteDependency(sourceDomain, targetId, targetDomain, targetRole)) {
                issues.add(issue("MANGO-ARCH-DEP-003", artifactId, targetId,
                        "starter-remote may only use its domain api/support and infra-feign"));
            }
        }
        return List.copyOf(issues);
    }

    private boolean isAllowedRemoteDependency(
            String sourceDomain, String targetId, String targetDomain, ModuleRole targetRole) {
        if (FEIGN_INFRA.equals(targetId)) {
            return true;
        }
        return sourceDomain.equals(targetDomain)
                && (targetRole == ModuleRole.API || targetRole == ModuleRole.SUPPORT);
    }

    private boolean isForbiddenResourceRuntimeDependency(
            String sourceArtifactId, Dependency dependency) {
        return RESOURCE_GROUP.equals(dependency.getGroupId())
                && RESOURCE_RUNTIME.contains(dependency.getArtifactId())
                && !sourceArtifactId.startsWith("mango-resource-")
                && !sourceArtifactId.endsWith("-app");
    }

    private boolean isMango(Dependency dependency) {
        return dependency.getGroupId() != null && dependency.getGroupId().startsWith("io.mango");
    }

    private boolean isArchitecturalDependency(
            Dependency dependency, Set<String> reactorArtifactIds) {
        return isMango(dependency) || reactorArtifactIds.contains(dependency.getArtifactId());
    }

    private boolean isTestOrProvided(Dependency dependency) {
        return "test".equals(dependency.getScope()) || "provided".equals(dependency.getScope());
    }

    private ArchitectureIssue issue(String ruleId, String source, String target, String message) {
        return new ArchitectureIssue(ruleId, source + " -> " + target, message);
    }
}
