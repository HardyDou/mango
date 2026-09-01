package io.mango.workflow.starter.resource;

import io.mango.resource.support.model.ResourceDeclaration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowMessageTemplateResourceProviderTest {

    private final WorkflowMessageTemplateResourceProvider provider =
            new WorkflowMessageTemplateResourceProvider();

    @Test
    void providesOnlyAssignedAndTerminalResultTemplates() {
        List<ResourceDeclaration> declarations = provider.provide();

        assertThat(declarations).hasSize(12);
        assertThat(declarations)
                .extracting(declaration -> field(declaration, "bizType"))
                .containsOnly(
                        "workflow.task.assigned",
                        "workflow.process.completed",
                        "workflow.process.rejected");
        assertThat(declarations)
                .allSatisfy(declaration -> {
                    String title = String.valueOf(field(declaration, "titleTemplate"));
                    String content = String.valueOf(field(declaration, "contentTemplate"));
                    assertThat(title).contains("{{processName}}");
                    assertThat(content).contains("{{applyTitle}}")
                            .doesNotContain("{{businessType}}", "{{businessKey}}", "{{definitionKey}}",
                                    "{{taskDefinitionKey}}");
                });
    }

    @Test
    void enablesSiteAndWecomChannelsByDefault() {
        Map<String, Boolean> channelDefaults = provider.provide().stream()
                .filter(declaration -> "workflow.task.assigned".equals(field(declaration, "bizType")))
                .collect(java.util.stream.Collectors.toMap(
                        declaration -> String.valueOf(field(declaration, "channelType")),
                        declaration -> (Boolean) field(declaration, "channelEnabled")));

        assertThat(channelDefaults).containsExactlyInAnyOrderEntriesOf(Map.of(
                "SITE", true,
                "WECOM", true,
                "EMAIL", false,
                "SMS", false));
    }

    private Object field(ResourceDeclaration declaration, String name) {
        return declaration.getFields().get(name).getValue();
    }
}
