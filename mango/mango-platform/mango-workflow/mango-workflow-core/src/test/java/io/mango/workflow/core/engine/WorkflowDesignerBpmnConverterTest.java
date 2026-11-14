package io.mango.workflow.core.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.workflow.core.model.WorkflowApprovalNodeConfig;
import io.mango.workflow.api.enums.WorkflowAssignmentMode;
import io.mango.workflow.api.enums.WorkflowAutoAssignmentStrategy;
import io.mango.workflow.api.enums.WorkflowNodeAffinityFallback;
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
                  "nodeAffinityFallback": "ROUND_ROBIN",
                  "assigneeIds": ["1001", "1002"]
                }
                """);

        assertThat(task.getAssignee()).isNull();
        assertThat(task.getLoopCharacteristics()).isNull();
        WorkflowApprovalNodeConfig config = new ObjectMapper().readValue(
                task.getExtensionElements().get("mangoApprovalConfig").get(0).getElementText(),
                WorkflowApprovalNodeConfig.class);
        assertThat(config.getAutoAssignmentStrategy()).isEqualTo(WorkflowAutoAssignmentStrategy.AFFINITY);
        assertThat(config.getNodeAffinityFallback()).isEqualTo(WorkflowNodeAffinityFallback.ROUND_ROBIN);
    }

    @Test
    void toModel_shouldExpandRoleCandidatesToMultiInstanceForOrSign() {
        UserTask task = approvalTask("""
                {
                  "assigneeType": "SPECIFIED_ROLE",
                  "roleIds": ["8173435341741372828"],
                  "approvalMode": "OR_SIGN"
                }
                """);

        MultiInstanceLoopCharacteristics loop = task.getLoopCharacteristics();
        assertThat(loop).isNotNull();
        assertThat(loop.isSequential()).isFalse();
        assertThat(loop.getInputDataItem())
                .isEqualTo("${mangoWorkflowAssigneeCollection.groupMembers('ROLE:8173435341741372828')}");
        assertThat(loop.getCompletionCondition()).isEqualTo("${nrOfCompletedInstances >= 1}");
        assertThat(task.getAssignee()).isEqualTo("${mangoAssignee_manager_approve}");
        assertThat(task.getCandidateGroups()).isEmpty();
    }

    @Test
    void toModel_shouldExpandPostCandidatesWithCountersignPassRatio() {
        UserTask task = approvalTask("""
                {
                  "assigneeType": "SPECIFIED_POST",
                  "postIds": ["100", "200"],
                  "approvalMode": "COUNTERSIGN",
                  "passRatio": 60
                }
                """);

        MultiInstanceLoopCharacteristics loop = task.getLoopCharacteristics();
        assertThat(loop.getInputDataItem())
                .isEqualTo("${mangoWorkflowAssigneeCollection.groupMembers('POST:100,POST:200')}");
        assertThat(loop.isSequential()).isFalse();
        assertThat(loop.getCompletionCondition()).isEqualTo(
                "${nrOfCompletedInstances >= mangoWorkflowApprovalThreshold.requiredApprovals(nrOfInstances, 60)}");
    }

    @Test
    void toModel_shouldExpandOrgCandidatesToSequentialMultiInstance() {
        UserTask task = approvalTask("""
                {
                  "assigneeType": "SPECIFIED_ORG",
                  "orgIds": ["5"],
                  "approvalMode": "SEQUENTIAL"
                }
                """);

        MultiInstanceLoopCharacteristics loop = task.getLoopCharacteristics();
        assertThat(loop.getInputDataItem())
                .isEqualTo("${mangoWorkflowAssigneeCollection.groupMembers('ORG:5')}");
        assertThat(loop.isSequential()).isTrue();
        assertThat(loop.getCompletionCondition()).isNull();
    }

    @Test
    void toModel_shouldExpandOrgLeaderCandidatesWithLeaderGroupPrefix() {
        UserTask task = approvalTask("""
                {
                  "assigneeType": "ORG_LEADER",
                  "orgIds": ["9"],
                  "orgLeaderUseInitiatorOrg": false,
                  "approvalMode": "OR_SIGN"
                }
                """);

        assertThat(task.getLoopCharacteristics().getInputDataItem())
                .isEqualTo("${mangoWorkflowAssigneeCollection.groupMembers('ORG_LEADER:9')}");
    }

    @Test
    void toModel_shouldKeepCandidateGroupForAutoAssignedRoleNode() {
        UserTask task = approvalTask("""
                {
                  "assigneeType": "SPECIFIED_ROLE",
                  "roleIds": ["817"],
                  "assignmentMode": "AUTO",
                  "autoAssignmentStrategy": "ROUND_ROBIN"
                }
                """);

        assertThat(task.getLoopCharacteristics()).isNull();
        assertThat(task.getAssignee()).isNull();
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
