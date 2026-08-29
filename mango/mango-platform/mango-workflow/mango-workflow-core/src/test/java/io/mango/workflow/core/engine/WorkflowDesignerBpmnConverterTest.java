package io.mango.workflow.core.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.workflow.core.model.WorkflowApprovalNodeConfig;
import io.mango.workflow.api.enums.WorkflowAssignmentMode;
import io.mango.workflow.api.enums.WorkflowAutoAssignmentStrategy;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WorkflowDesignerBpmnConverterTest {

    private final WorkflowDesignerBpmnConverter converter = new WorkflowDesignerBpmnConverter(
            new ObjectMapper(),
            new WorkflowAssigneeResolver(mock(JdbcTemplate.class)));

    @Test
    void toModel_shouldApplyCountersignPassRatioCompletionCondition() {
        UserTask task = approvalTask("""
                {
                  "approvalMode": "COUNTERSIGN",
                  "passRatio": 50,
                  "assigneeIds": ["admin", "zhangsan", "lisi"]
                }
                """);

        MultiInstanceLoopCharacteristics loop = task.getLoopCharacteristics();

        assertThat(loop).isNotNull();
        assertThat(loop.getCompletionCondition())
                .isEqualTo("${nrOfCompletedInstances >= mangoWorkflowApprovalThreshold.requiredApprovals(nrOfInstances, 50)}");
    }

    @Test
    void toModel_shouldClampCountersignPassRatioInCompletionCondition() {
        UserTask task = approvalTask("""
                {
                  "approvalMode": "COUNTERSIGN",
                  "passRatio": 150,
                  "assigneeIds": ["admin", "zhangsan"]
                }
                """);

        assertThat(task.getLoopCharacteristics().getCompletionCondition())
                .isEqualTo("${nrOfCompletedInstances >= mangoWorkflowApprovalThreshold.requiredApprovals(nrOfInstances, 100)}");
    }

    @Test
    void toModel_shouldKeepOrSignCompletionCondition() {
        UserTask task = approvalTask("""
                {
                  "approvalMode": "OR_SIGN",
                  "passRatio": 80,
                  "assigneeIds": ["admin", "zhangsan"]
                }
                """);

        assertThat(task.getLoopCharacteristics().getCompletionCondition())
                .isEqualTo("${nrOfCompletedInstances >= 1}");
    }

    @Test
    void toModel_shouldKeepActionDisabledAndTooltipInApprovalConfigExtension() throws Exception {
        UserTask task = approvalTask("""
                {
                  "assigneeIds": ["admin"],
                  "actions": {
                    "transfer": {
                      "enabled": true,
                      "disabled": true,
                      "tooltip": "当前节点暂不允许转办"
                    }
                  }
                }
                """);

        List<ExtensionElement> elements = task.getExtensionElements().get("mangoApprovalConfig");
        WorkflowApprovalNodeConfig config = new ObjectMapper()
                .readValue(elements.get(0).getElementText(), WorkflowApprovalNodeConfig.class);

        assertThat(config.getActions().get("transfer").getDisabled()).isTrue();
        assertThat(config.getActions().get("transfer").getTooltip()).isEqualTo("当前节点暂不允许转办");
    }

    @Test
    void toModel_shouldDefaultLegacyApprovalConfigToClaim() throws Exception {
        UserTask task = approvalTask("""
                {
                  "assigneeIds": ["1001"]
                }
                """);

        List<ExtensionElement> elements = task.getExtensionElements().get("mangoApprovalConfig");
        WorkflowApprovalNodeConfig config = new ObjectMapper()
                .readValue(elements.get(0).getElementText(), WorkflowApprovalNodeConfig.class);

        assertThat(config.getAssignmentMode()).isEqualTo(WorkflowAssignmentMode.CLAIM);
        assertThat(task.getAssignee()).isEqualTo("1001");
    }

    @Test
    void toModel_shouldDeferAutoAssignmentToRuntime() throws Exception {
        UserTask task = approvalTask("""
                {
                  "assignmentMode": "AUTO",
                  "autoAssignmentStrategy": "AFFINITY",
                  "assigneeIds": ["1001", "1002"]
                }
                """);

        assertThat(task.getAssignee()).isNull();
        assertThat(task.getLoopCharacteristics()).isNull();
        WorkflowApprovalNodeConfig config = new ObjectMapper().readValue(
                task.getExtensionElements().get("mangoApprovalConfig").get(0).getElementText(),
                WorkflowApprovalNodeConfig.class);
        assertThat(config.getAutoAssignmentStrategy()).isEqualTo(WorkflowAutoAssignmentStrategy.AFFINITY);
    }

    private UserTask approvalTask(String approvalConfigJson) {
        String designerJson = """
                {
                  "id": "root",
                  "nodeType": "ROOT",
                  "nodeName": "发起",
                  "childNode": {
                    "id": "manager_approve",
                    "nodeType": "APPROVAL",
                    "nodeName": "经理审批",
                    "properties": {
                      "approvalConfig": %s
                    }
                  }
                }
                """.formatted(approvalConfigJson);
        BpmnModel model = converter.toModel(designerJson, "test_process", "测试流程");
        return (UserTask) model.getMainProcess().getFlowElement("manager_approve");
    }
}
