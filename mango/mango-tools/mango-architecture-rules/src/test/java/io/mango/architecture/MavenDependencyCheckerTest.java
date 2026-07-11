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
                .containsExactly("MANGO-ARCH-DEP-005");
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
    void supportDependingOnCoreIsRejected() {
        assertThat(checker.check("mango-order-support", List.of(dependency("mango-order-core"))))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-DEP-006");
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
