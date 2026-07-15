package io.mango.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

class MavenDependencyCheckerTest {

    private final MavenDependencyChecker checker = new MavenDependencyChecker();

    @Test
    void legalLayerDependenciesPass() {
        assertThat(checker.check("mango-order-api", List.of(dependency("mango-common"))))
                .isEmpty();
        assertThat(checker.check("mango-order-core", List.of(dependency("mango-order-api"))))
                .isEmpty();
        assertThat(checker.check("mango-order-starter-remote", List.of(
                dependency("mango-order-api"), dependency("mango-infra-feign-starter"))))
                .isEmpty();
    }

    @Test
    void apiDependingOnCoreIsRejected() {
        assertThat(checker.check("mango-order-api", List.of(dependency("mango-order-core"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-001");
    }

    @Test
    void coreDependingOnAnotherDomainCoreOrStarterIsRejected() {
        assertThat(checker.check("mango-order-core", List.of(
                dependency("mango-user-core"), dependency("mango-user-starter"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-002", "MANGO-ARCH-DEP-002");
    }

    @Test
    void coreDependingOnItsOwnStarterIsRejected() {
        assertThat(checker.check("mango-order-core", List.of(
                dependency("mango-order-starter"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-002");
    }

    @Test
    void starterRemoteDependingOnForeignMangoModuleIsRejected() {
        assertThat(checker.check("mango-order-starter-remote", List.of(
                dependency("mango-user-api"), dependency("mango-user-support"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-003", "MANGO-ARCH-DEP-003");
    }

    @Test
    void starterDependingOnForeignStarterIsRejected() {
        assertThat(checker.check("mango-admin-starter", List.of(
                dependency("mango-resource-starter"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-007", "MANGO-ARCH-DEP-005");
    }

    @Test
    void starterDependingOnForeignCoreOrBusinessStarterIsRejected() {
        assertThat(checker.check("mango-order-starter", List.of(
                dependency("mango-billing-core"), dependency("mango-billing-starter"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-007", "MANGO-ARCH-DEP-007");
    }

    @Test
    void adapterSpecificStartersMayUseTheirDomainCore() {
        assertThat(checker.check("mango-access-web-starter", List.of(
                dependency("mango-access-api"), dependency("mango-access-core"))))
                .isEmpty();
        assertThat(checker.check("mango-access-gateway-starter", List.of(
                dependency("mango-access-core"))))
                .isEmpty();
    }

    @Test
    void adapterSpecificStarterMayNotUseAnotherDomainCore() {
        assertThat(checker.check("mango-access-web-starter", List.of(
                dependency("mango-access-control-core"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-007");
        assertThat(checker.check("mango-order-admin-starter", List.of(
                dependency("mango-order-core"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-007");
    }

    @Test
    void starterMayUseExplicitInfrastructureStarter() {
        Dependency dependency = dependency("mango-infra-web-starter");
        dependency.setGroupId("io.mango.infra.web");

        assertThat(checker.check("mango-order-starter", List.of(dependency))).isEmpty();
    }

    @Test
    void securityRemoteAllowsOnlyItsDocumentedAggregateStarters() {
        assertThat(checker.check("mango-security-starter-remote", List.of(
                dependency("mango-infra-security-starter"),
                dependency("mango-auth-starter-remote"),
                dependency("mango-identity-starter-remote"),
                dependency("mango-authorization-starter-remote"))))
                .isEmpty();
        assertThat(checker.check("mango-security-starter-remote", List.of(
                dependency("mango-order-starter-remote"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-003");
    }

    @Test
    void appCannotDependDirectlyOnCore() {
        assertThat(checker.check("mango-business-app", List.of(
                dependency("mango-order-core"), dependency("mango-order-starter"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-008");
    }

    @Test
    void businessGroupReactorDependencyUsesTheSameLayerRules() {
        Dependency dependency = dependency("guarantee-config-core");
        dependency.setGroupId("com.yunxinbaokeji.baohan");

        assertThat(checker.check(
                "guarantee-config-api", List.of(dependency), Set.of("guarantee-config-core")))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-001");
    }

    @Test
    void publishedDependencyInGovernedBusinessGroupUsesLayerRules() {
        Dependency dependency = dependency("billing-core");
        dependency.setGroupId("com.example.business.billing");

        assertThat(checker.check(
                "order-api",
                List.of(dependency),
                Set.of(),
                Set.of("com.example.business")))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-001");
    }

    @Test
    void supportDependingOnCoreIsRejected() {
        assertThat(checker.check("mango-order-support", List.of(dependency("mango-order-core"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-006");
    }

    @Test
    void providedScopeCannotBypassLayerRules() {
        Dependency core = dependency("mango-order-core");
        core.setScope("provided");
        Dependency openFeign = dependency("spring-cloud-starter-openfeign");
        openFeign.setScope("provided");

        assertThat(checker.check("mango-order-api", List.of(core)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-001");
        assertThat(checker.check("mango-order-starter-remote", List.of(openFeign)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-004");
    }

    @Test
    void testScopeIsExcludedFromProductionLayerRules() {
        Dependency core = dependency("mango-order-core");
        core.setScope("test");

        assertThat(checker.check("mango-order-api", List.of(core))).isEmpty();
    }

    private Dependency dependency(String artifactId) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(artifactId.startsWith("mango-resource-")
                ? "io.mango.platform.resource" : "io.mango.platform");
        dependency.setArtifactId(artifactId);
        dependency.setVersion("1.0.0-SNAPSHOT");
        return dependency;
    }
}
