package io.mango.workflow.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowResourceDeclarationContractTest {

    private static final String DEMO_ROOT = "META-INF/mango/demo-assets/workflow/";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void demoDeclarations_areIsolatedAndInitOnly() throws IOException {
        String declaration = resourceText("META-INF/mango/demo/workflow-demo-definition.yml");

        assertThat(declaration)
                .contains("module-code: workflow")
                .contains("definitionKey: { type: STRING, value: expense_reimbursement }")
                .contains("definitionKey: { type: STRING, value: contract_seal_approval }")
                .contains("definitionKey: { type: STRING, value: leave_application }");
        assertThat(count(declaration, "sync-mode: INIT_ONLY")).isEqualTo(3);
        assertThat(count(declaration, "- id: \"")).isEqualTo(3);
    }

    @Test
    void demoAssets_areValidAndKeepExpectedProcessShape() throws IOException {
        JsonNode expense = json("expense-reimbursement-designer.json");
        JsonNode contract = json("contract-seal-approval-designer.json");
        JsonNode leave = json("leave-application-designer.json");

        assertThat(nodeIds(expense)).containsExactly("manager_approve", "finance_review");
        assertThat(nodeIds(contract)).containsExactly(
                "dept_manager_approve", "legal_review", "finance_review", "seal_keeper");
        assertThat(nodeIds(leave)).containsExactly("leave_manager_approve", "hr_record");
        assertThat(json("expense-reimbursement-form.json").path("customConfig").path("applyPageKey").asText())
                .isEqualTo("workflow.expense.apply");
        assertThat(json("contract-seal-approval-form.json").path("customConfig").path("applyPageKey").asText())
                .isEqualTo("workflow.contractSeal.apply");
        assertThat(json("leave-application-form.json").path("fields")).hasSize(5);
    }

    @Test
    void approvalCenterBelongsToPlatformCapabilitiesAndKeepsRoutes() throws IOException {
        JsonNode declaration = objectMapper.readTree(
                resourceText("META-INF/mango/resources/workflow-common-menu.json"));

        JsonNode approvalCenter = findMenu(declaration, "workflow");
        assertThat(approvalCenter).isNotNull();
        assertThat(approvalCenter.path("parentCode").asText()).isEqualTo("data");
        assertThat(approvalCenter.path("path").asText()).isEqualTo("/workflow");
        assertThat(approvalCenter.path("redirect").asText()).isEqualTo("/workflow/start-process");
    }

    private List<String> nodeIds(JsonNode root) {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        JsonNode current = root.path("childNode");
        while (!current.isMissingNode() && !current.isNull()) {
            ids.add(current.path("id").asText());
            current = current.path("childNode");
        }
        return ids;
    }

    private JsonNode findMenu(JsonNode node, String menuCode) {
        if (node.isObject() && menuCode.equals(node.path("menuCode").asText())) {
            return node;
        }
        for (JsonNode child : node) {
            JsonNode match = findMenu(child, menuCode);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private JsonNode json(String name) throws IOException {
        return objectMapper.readTree(resourceText(DEMO_ROOT + name));
    }

    private int count(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }

    private String resourceText(String path) throws IOException {
        try (var input = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
