package io.mango.architecture;

import org.apache.maven.model.Dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Enforcer-compatible dependency policy implemented from Maven's resolved model. */
public final class MavenDependencyChecker {

    private static final String FEIGN_INFRA = "mango-infra-feign-starter";
    private static final String OPENFEIGN_STARTER = "spring-cloud-starter-openfeign";
    private static final String SECURITY_REMOTE = "mango-security-starter-remote";
    private static final String MANGO_COMMON = "mango-common";
    private static final Set<ModuleRole> IMPLEMENTATION_ROLES =
            Set.of(ModuleRole.CORE, ModuleRole.STARTER, ModuleRole.STARTER_REMOTE);
    private static final Set<ModuleRole> API_FORBIDDEN_ROLES =
            Set.of(
                    ModuleRole.CORE,
                    ModuleRole.SUPPORT,
                    ModuleRole.STARTER,
                    ModuleRole.STARTER_REMOTE);
    private static final Set<ModuleRole> APP_ALLOWED_ROLES =
            Set.of(ModuleRole.STARTER, ModuleRole.STARTER_REMOTE);
    private static final Set<String> STARTER_ADAPTER_SUFFIXES =
            Set.of("-web", "-gateway");
    private static final Set<String> SECURITY_REMOTE_AGGREGATES =
            Set.of(
                    "mango-infra-security-starter",
                    "mango-auth-starter-remote",
                    "mango-identity-starter-remote",
                    "mango-authorization-starter-remote");
    private static final String RESOURCE_GROUP = "io.mango.platform.resource";
    private static final Set<String> RESOURCE_RUNTIME =
            Set.of(
                    "mango-resource-core",
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
        return check(artifactId, dependencies, reactorArtifactIds, Set.of());
    }

    public List<ArchitectureIssue> check(
            String artifactId,
            Collection<Dependency> dependencies,
            Set<String> reactorArtifactIds,
            Set<String> architecturalGroupPrefixes) {
        List<ArchitectureIssue> issues = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            if (isTestDependency(dependency)) {
                continue;
            }
            DependencyContext context = dependencyContext(artifactId, dependency);
            if (checkDirectOpenFeign(context, issues)) {
                continue;
            }
            if (!isArchitecturalDependency(
                    dependency, reactorArtifactIds, architecturalGroupPrefixes)) {
                continue;
            }
            checkApiDependency(context, issues);
            checkCoreDependency(context, issues);
            checkSupportDependency(context, issues);
            checkStarterDependency(context, issues);
            checkResourceDependency(context, issues);
            checkRemoteDependency(context, issues);
            checkAppDependency(context, issues);
        }
        return List.copyOf(issues);
    }

    private DependencyContext dependencyContext(String sourceArtifactId, Dependency dependency) {
        String targetArtifactId = dependency.getArtifactId();
        return new DependencyContext(
                sourceArtifactId,
                ModuleRole.fromArtifactId(sourceArtifactId),
                ModuleRole.domainOf(sourceArtifactId),
                dependency,
                targetArtifactId,
                ModuleRole.fromArtifactId(targetArtifactId),
                ModuleRole.domainOf(targetArtifactId));
    }

    private boolean checkDirectOpenFeign(
            DependencyContext context, List<ArchitectureIssue> issues) {
        if (context.sourceRole() != ModuleRole.STARTER_REMOTE
                || !OPENFEIGN_STARTER.equals(context.targetArtifactId())) {
            return false;
        }
        issues.add(
                issue(
                        "MANGO-ARCH-DEP-004",
                        context.sourceArtifactId(),
                        context.targetArtifactId(),
                        "starter-remote must use mango-infra-feign-starter"));
        return true;
    }

    private void checkApiDependency(DependencyContext context, List<ArchitectureIssue> issues) {
        if (context.sourceRole() != ModuleRole.API
                || !API_FORBIDDEN_ROLES.contains(context.targetRole())) {
            return;
        }
        issues.add(
                issue(
                        "MANGO-ARCH-DEP-001",
                        context.sourceArtifactId(),
                        context.targetArtifactId(),
                        "api must not depend on core/support/starter"));
    }

    private void checkCoreDependency(DependencyContext context, List<ArchitectureIssue> issues) {
        if (context.sourceRole() != ModuleRole.CORE
                || !IMPLEMENTATION_ROLES.contains(context.targetRole())) {
            return;
        }
        issues.add(
                issue(
                        "MANGO-ARCH-DEP-002",
                        context.sourceArtifactId(),
                        context.targetArtifactId(),
                        "core must depend on contracts/support, never core/starter"));
    }

    private void checkSupportDependency(DependencyContext context, List<ArchitectureIssue> issues) {
        if (context.sourceRole() != ModuleRole.SUPPORT
                || !IMPLEMENTATION_ROLES.contains(context.targetRole())) {
            return;
        }
        issues.add(
                issue(
                        "MANGO-ARCH-DEP-006",
                        context.sourceArtifactId(),
                        context.targetArtifactId(),
                        "support must not depend on core/starter"));
    }

    private void checkStarterDependency(DependencyContext context, List<ArchitectureIssue> issues) {
        if (context.sourceRole() != ModuleRole.STARTER
                || belongsToSameDomain(context)) {
            return;
        }
        if (!isForbiddenStarterTarget(context)) {
            return;
        }
        issues.add(
                issue(
                        "MANGO-ARCH-DEP-007",
                        context.sourceArtifactId(),
                        context.targetArtifactId(),
                        "starter may only depend on its domain api/core and explicit infra"
                                + " starters"));
    }

    private boolean belongsToSameDomain(DependencyContext context) {
        if (context.sourceDomain().equals(context.targetDomain())) {
            return true;
        }
        if (!Set.of(ModuleRole.API, ModuleRole.CORE, ModuleRole.SUPPORT)
                .contains(context.targetRole())) {
            return false;
        }
        return STARTER_ADAPTER_SUFFIXES.stream()
                .anyMatch(suffix -> context.sourceDomain().equals(context.targetDomain() + suffix));
    }

    private boolean isForbiddenStarterTarget(DependencyContext context) {
        if (context.targetRole() == ModuleRole.CORE) {
            return true;
        }
        if (!Set.of(ModuleRole.STARTER, ModuleRole.STARTER_REMOTE).contains(context.targetRole())) {
            return false;
        }
        return !isInfrastructureDependency(context.dependency());
    }

    private void checkResourceDependency(
            DependencyContext context, List<ArchitectureIssue> issues) {
        if (!isForbiddenResourceRuntimeDependency(
                context.sourceArtifactId(), context.dependency())) {
            return;
        }
        issues.add(
                issue(
                        "MANGO-ARCH-DEP-005",
                        context.sourceArtifactId(),
                        context.targetArtifactId(),
                        "non-resource library must depend on mango-resource-api, not"
                                + " resource runtime"));
    }

    private void checkRemoteDependency(DependencyContext context, List<ArchitectureIssue> issues) {
        if (context.sourceRole() != ModuleRole.STARTER_REMOTE) {
            return;
        }
        if (isAllowedRemoteDependency(context)) {
            return;
        }
        issues.add(
                issue(
                        "MANGO-ARCH-DEP-003",
                        context.sourceArtifactId(),
                        context.targetArtifactId(),
                        "starter-remote may only use its domain api/support and infra-feign"));
    }

    private void checkAppDependency(DependencyContext context, List<ArchitectureIssue> issues) {
        if (context.sourceRole() != ModuleRole.APP) {
            return;
        }
        if (MANGO_COMMON.equals(context.targetArtifactId())
                || APP_ALLOWED_ROLES.contains(context.targetRole())) {
            return;
        }
        issues.add(
                issue(
                        "MANGO-ARCH-DEP-008",
                        context.sourceArtifactId(),
                        context.targetArtifactId(),
                        "app may depend on Mango starters and mango-common only"));
    }

    private boolean isAllowedRemoteDependency(DependencyContext context) {
        if (FEIGN_INFRA.equals(context.targetArtifactId())) {
            return true;
        }
        if (SECURITY_REMOTE.equals(context.sourceArtifactId())
                && SECURITY_REMOTE_AGGREGATES.contains(context.targetArtifactId())) {
            return true;
        }
        return context.sourceDomain().equals(context.targetDomain())
                && Set.of(ModuleRole.API, ModuleRole.SUPPORT).contains(context.targetRole());
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

    private boolean isInfrastructureDependency(Dependency dependency) {
        return dependency.getGroupId() != null
                && dependency.getGroupId().startsWith("io.mango.infra");
    }

    private boolean isArchitecturalDependency(
            Dependency dependency,
            Set<String> reactorArtifactIds,
            Set<String> architecturalGroupPrefixes) {
        return isMango(dependency)
                || reactorArtifactIds.contains(dependency.getArtifactId())
                || belongsToGovernedGroup(dependency, architecturalGroupPrefixes);
    }

    private boolean belongsToGovernedGroup(
            Dependency dependency, Set<String> architecturalGroupPrefixes) {
        String groupId = dependency.getGroupId();
        if (groupId == null) {
            return false;
        }
        return architecturalGroupPrefixes.stream()
                .filter(prefix -> prefix != null && !prefix.isBlank())
                .map(String::trim)
                .anyMatch(prefix -> groupId.equals(prefix) || groupId.startsWith(prefix + "."));
    }

    private boolean isTestDependency(Dependency dependency) {
        return "test".equals(dependency.getScope());
    }

    private ArchitectureIssue issue(String ruleId, String source, String target, String message) {
        return new ArchitectureIssue(ruleId, source + " -> " + target, message);
    }

    private record DependencyContext(
            String sourceArtifactId,
            ModuleRole sourceRole,
            String sourceDomain,
            Dependency dependency,
            String targetArtifactId,
            ModuleRole targetRole,
            String targetDomain) {}
}
