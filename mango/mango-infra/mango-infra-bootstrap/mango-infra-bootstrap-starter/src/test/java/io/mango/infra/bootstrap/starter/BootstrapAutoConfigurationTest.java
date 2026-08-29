package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapMode;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BootstrapAutoConfigurationTest {

    @Test
    void normalBootstrapKeepsAllContributors() {
        BootstrapStepContributor ordinary = List::of;
        BootstrapStepContributor eligible = eligibleContributor();

        assertThat(BootstrapAutoConfiguration.selectContributors(
                new BootstrapProperties(), List.of(ordinary, eligible)))
                .containsExactly(ordinary, eligible);
    }

    @Test
    void resourceBaselineBuildKeepsOnlyExplicitlyEligibleContributors() {
        BootstrapStepContributor ordinary = List::of;
        BootstrapStepContributor eligible = eligibleContributor();

        assertThat(BootstrapAutoConfiguration.selectContributors(
                resourceBaselineProperties(), List.of(ordinary, eligible)))
                .containsExactly(eligible);
    }

    @Test
    void resourceBaselineBuildRejectsUnsafeInvocationAndEmptySelection() {
        BootstrapProperties unsafe = resourceBaselineProperties();
        unsafe.setEnvironmentKey("production");

        assertThatThrownBy(() -> BootstrapAutoConfiguration.selectContributors(
                unsafe, List.of(eligibleContributor())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reserved environment key");
        assertThatThrownBy(() -> BootstrapAutoConfiguration.selectContributors(
                resourceBaselineProperties(), List.of(List::of)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("eligible Resource Bootstrap contributor");
    }

    private static BootstrapProperties resourceBaselineProperties() {
        BootstrapProperties properties = new BootstrapProperties();
        properties.setMode(BootstrapMode.BOOTSTRAP);
        properties.setAction(BootstrapAction.APPLY);
        properties.setEnvironmentKey("mango-resource-baseline-build");
        properties.setResourceBaselineBuildEnabled(true);
        return properties;
    }

    private static BootstrapStepContributor eligibleContributor() {
        return new BootstrapStepContributor() {
            @Override
            public List<io.mango.infra.bootstrap.api.BootstrapStep> contributeSteps() {
                return List.of();
            }

            @Override
            public boolean supportsResourceBaselineBuild() {
                return true;
            }
        };
    }
}
