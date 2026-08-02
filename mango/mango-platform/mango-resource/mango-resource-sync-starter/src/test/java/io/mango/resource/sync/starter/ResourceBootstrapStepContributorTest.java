package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.enums.ResourceApplyMode;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.model.ResourceDeclaration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceBootstrapStepContributorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void contributesRequiredAndFinalizeStepsWithGenerationFence() throws Exception {
        ResourceDeclaration second = declaration("resource-2", "module-b");
        ResourceDeclaration first = declaration("resource-1", "module-a");
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.getRemote().setAppCode("app");
        properties.getRemote().setServiceCode("service");
        List<RegisterResourceDeclarationsCommand> commands = new ArrayList<>();
        ResourceDeclarationApi api = command -> {
            commands.add(command);
            return R.ok(true);
        };
        ResourceBootstrapStepContributor contributor = new ResourceBootstrapStepContributor(
                properties, collector(() -> List.of(second, first)), api,
                new ResourceManifestSerializer(),
                new ResourceDeclarationCanonicalizer(objectMapper), "fallback");

        List<BootstrapStep> steps = contributor.contributeSteps();

        assertThat(steps).extracting(BootstrapStep::code)
                .containsExactly("RESOURCE_REQUIRED", "RESOURCE_FINALIZE");
        assertThat(steps).extracting(BootstrapStep::phase)
                .containsExactly(BootstrapPhase.EXPAND, BootstrapPhase.FINALIZE);
        assertThat(steps.get(0).optionalDependencies()).containsExactly("FLYWAY_EXPAND");
        assertThat(steps.get(1).dependencies()).containsExactly("RESOURCE_REQUIRED");

        steps.get(0).execute(context(BootstrapPhase.EXPAND));
        steps.get(1).execute(context(BootstrapPhase.FINALIZE));

        assertThat(commands).extracting(RegisterResourceDeclarationsCommand::getApplyMode)
                .containsExactly(ResourceApplyMode.EXPAND, ResourceApplyMode.FINALIZE);
        assertThat(commands).allSatisfy(command -> {
            assertThat(command.getAppCode()).isEqualTo("app");
            assertThat(command.getServiceCode()).isEqualTo("service");
            assertThat(command.getModuleCodes()).containsExactly("module-a", "module-b");
            assertThat(command.getEnvironmentKey()).isEqualTo("test");
            assertThat(command.getGeneration()).isEqualTo(8L);
            assertThat(command.getManifestFingerprint()).isEqualTo("f".repeat(64));
            assertThat(command.getFencingToken()).isEqualTo(13L);
        });
        JsonNode declarations = objectMapper.readTree(commands.get(0).getDeclarations());
        assertThat(declarations).extracting(node -> node.get("id").asText())
                .containsExactly("resource-1", "resource-2");
    }

    @Test
    void omitsDisabledResourceCapabilityAndRejectsFailedPublication() {
        ResourceRegistryProperties disabled = new ResourceRegistryProperties();
        disabled.setEnabled(false);
        ResourceBootstrapStepContributor disabledContributor = new ResourceBootstrapStepContributor(
                disabled, collector(() -> List.of(declaration("resource-1", "module-a"))),
                command -> R.ok(true), new ResourceManifestSerializer(),
                new ResourceDeclarationCanonicalizer(objectMapper), "app");
        assertThat(disabledContributor.contributeSteps()).isEmpty();

        ResourceRegistryProperties enabled = new ResourceRegistryProperties();
        ResourceBootstrapStepContributor failingContributor = new ResourceBootstrapStepContributor(
                enabled, collector(() -> List.of(declaration("resource-1", "module-a"))),
                command -> R.fail("target unavailable"), new ResourceManifestSerializer(),
                new ResourceDeclarationCanonicalizer(objectMapper), "app");
        BootstrapStep required = failingContributor.contributeSteps().get(0);

        assertThatThrownBy(() -> required.execute(context(BootstrapPhase.EXPAND)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Resource bootstrap did not complete")
                .hasMessageContaining("target unavailable");
    }

    @Test
    void fingerprintUsesSemanticContentInsteadOfPhysicalResourceDescription() {
        ResourceDeclaration exploded = declaration("resource-1", "module-a");
        exploded.setSource("file [/workspace/target/classes/META-INF/mango/resources/module.yml]");
        ResourceDeclaration nested = exploded.copy();
        nested.setSource("URL [jar:nested:/app.jar/!BOOT-INF/lib/module.jar!/META-INF/mango/resources/module.yml]");

        String explodedFingerprint = fingerprintMaterial(exploded);
        String nestedFingerprint = fingerprintMaterial(nested);
        nested.setName("changed");

        assertThat(nestedFingerprint).isEqualTo(explodedFingerprint);
        assertThat(fingerprintMaterial(nested)).isNotEqualTo(explodedFingerprint);
    }

    private String fingerprintMaterial(ResourceDeclaration declaration) {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        ResourceBootstrapStepContributor contributor = new ResourceBootstrapStepContributor(
                properties, collector(() -> List.of(declaration)), command -> R.ok(true), new ResourceManifestSerializer(),
                new ResourceDeclarationCanonicalizer(objectMapper), "app");
        return contributor.contributeSteps().get(0).fingerprintMaterial();
    }

    private static ResourceDeclarationCollector collector(ResourceProvider provider) {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("resourceProvider", provider);
        return new ResourceDeclarationCollector(beans.getBeanProvider(ResourceProvider.class));
    }

    private static ResourceDeclaration declaration(String id, String moduleCode) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType("WORKFLOW_DEFINITION");
        declaration.setModuleCode(moduleCode);
        declaration.setBizKey(id);
        declaration.setName(id);
        return declaration;
    }

    private static BootstrapExecutionContext context(BootstrapPhase phase) {
        return new BootstrapExecutionContext("execution", "test", "release", "revision",
                8L, "f".repeat(64), 13L, phase);
    }
}
