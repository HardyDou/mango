package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapStepResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BootstrapPlanBuilderTest {

    private final BootstrapPlanBuilder builder = new BootstrapPlanBuilder(new BootstrapManifestHasher());

    @Test
    void shouldOrderRequiredAndInstalledOptionalDependencies() {
        BootstrapPlan plan = builder.build("release", "revision", List.of(contributor(
                step("resource", Set.of(), Set.of("flyway"), "resource-v1"),
                step("tenant", Set.of("resource"), Set.of(), "tenant-v1"),
                step("flyway", Set.of(), Set.of(), "flyway-v1"))));

        assertThat(plan.steps()).extracting(BootstrapStep::code)
                .containsExactly("flyway", "resource", "tenant");
    }

    @Test
    void shouldAllowMissingOptionalDependency() {
        BootstrapPlan plan = builder.build("release", "revision", List.of(contributor(
                step("resource", Set.of(), Set.of("flyway"), "resource-v1"))));

        assertThat(plan.steps()).extracting(BootstrapStep::code).containsExactly("resource");
    }

    @Test
    void shouldRejectMissingRequiredDependency() {
        assertThatThrownBy(() -> builder.build("release", "revision", List.of(contributor(
                step("resource", Set.of("flyway"), Set.of(), "resource-v1")))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing bootstrap step dependency")
                .hasMessageContaining("flyway");
    }

    @Test
    void shouldRejectCycleAndDuplicateCodes() {
        assertThatThrownBy(() -> builder.build("release", "revision", List.of(contributor(
                step("one", Set.of("two"), Set.of(), "one-v1"),
                step("two", Set.of("one"), Set.of(), "two-v1")))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cyclic bootstrap step dependencies");

        assertThatThrownBy(() -> builder.build("release", "revision", List.of(contributor(
                step("one", Set.of(), Set.of(), "one-v1"),
                step("one", Set.of(), Set.of(), "one-v1")))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate bootstrap step code");
    }

    @Test
    void shouldProduceStableFingerprintAndDetectMaterialChanges() {
        BootstrapPlan first = builder.build("release", "revision", List.of(contributor(
                step("two", Set.of("one"), Set.of(), "two-v1"),
                step("one", Set.of(), Set.of(), "one-v1"))));
        BootstrapPlan reordered = builder.build("release", "revision", List.of(contributor(
                step("one", Set.of(), Set.of(), "one-v1"),
                step("two", Set.of("one"), Set.of(), "two-v1"))));
        BootstrapPlan changed = builder.build("release", "revision", List.of(contributor(
                step("one", Set.of(), Set.of(), "one-v2"),
                step("two", Set.of("one"), Set.of(), "two-v1"))));

        assertThat(reordered.manifestFingerprint()).isEqualTo(first.manifestFingerprint());
        assertThat(changed.manifestFingerprint()).isNotEqualTo(first.manifestFingerprint());
    }

    private static BootstrapStepContributor contributor(BootstrapStep... steps) {
        return () -> List.of(steps);
    }

    private static BootstrapStep step(String code, Set<String> dependencies,
                                      Set<String> optionalDependencies, String material) {
        return new BootstrapStep() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public BootstrapPhase phase() {
                return BootstrapPhase.EXPAND;
            }

            @Override
            public Set<String> dependencies() {
                return dependencies;
            }

            @Override
            public Set<String> optionalDependencies() {
                return optionalDependencies;
            }

            @Override
            public String fingerprintMaterial() {
                return material;
            }

            @Override
            public BootstrapStepResult execute(BootstrapExecutionContext context) {
                return BootstrapStepResult.completed(code);
            }
        };
    }
}
