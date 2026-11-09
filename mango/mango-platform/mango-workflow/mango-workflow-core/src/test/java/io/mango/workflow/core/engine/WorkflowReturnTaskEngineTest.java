package io.mango.workflow.core.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.workflow.core.support.WorkflowReturnTargetSupport;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 真实 Flowable 引擎下的退回基线：验证排他网关分支加汇聚的流程无需回退连线即可退回历史节点。
 */
class WorkflowReturnTaskEngineTest {

    private static final String PROCESS_KEY = "risk_review";

    private final WorkflowDesignerBpmnConverter converter = new WorkflowDesignerBpmnConverter(
            new ObjectMapper(),
            new WorkflowAssigneeResolver(mock(JdbcTemplate.class)));

    private ProcessEngine processEngine;
    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        processEngine = new StandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:workflow-return-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setAsyncExecutorActivate(false)
                .buildProcessEngine();
        repositoryService = processEngine.getRepositoryService();
        runtimeService = processEngine.getRuntimeService();
        taskService = processEngine.getTaskService();
    }

    @AfterEach
    void tearDown() {
        processEngine.close();
    }

    @Test
    void returningToHistoricNodeCrossesExclusiveGatewayBranchWithoutBackwardSequenceFlow() {
        deployGatewayReviewProcess();
        String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY,
                Map.of("needInitialReview", true)).getId();

        Task initialReview = singleTask(processInstanceId);
        assertThat(initialReview.getTaskDefinitionKey()).isEqualTo("risk_specialist_review");
        taskService.complete(initialReview.getId());

        Task supervisorReview = singleTask(processInstanceId);
        assertThat(supervisorReview.getTaskDefinitionKey()).isEqualTo("risk_supervisor_review");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo("risk_supervisor_review", "risk_specialist_review")
                .changeState();

        Task returnedTask = singleTask(processInstanceId);
        assertThat(returnedTask.getTaskDefinitionKey()).isEqualTo("risk_specialist_review");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()).isEqualTo(1L);
    }

    @Test
    void movingToActivityWithoutActiveExecutionFailsInsteadOfSilentlySucceeding() {
        deployGatewayReviewProcess();
        String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY,
                Map.of("needInitialReview", false)).getId();

        assertThat(singleTask(processInstanceId).getTaskDefinitionKey()).isEqualTo("risk_supervisor_review");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(processInstanceId)
                        .moveActivityIdTo("not_active_activity", "risk_specialist_review")
                        .changeState())
                .hasMessageContaining("not_active_activity");
    }

    @Test
    void mergeGatewayTargetResolvesToUpstreamCompletedUserTask() {
        deployGatewayReviewProcess();
        String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY,
                Map.of("needInitialReview", true)).getId();
        taskService.complete(singleTask(processInstanceId).getId());
        Task supervisorReview = singleTask(processInstanceId);
        assertThat(supervisorReview.getTaskDefinitionKey()).isEqualTo("risk_supervisor_review");

        String resolved = WorkflowReturnTargetSupport.resolveTarget(
                processEngine.getHistoryService(), repositoryService, supervisorReview,
                "risk_review_type_branch_merge");
        assertThat(resolved).isEqualTo("risk_specialist_review");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(supervisorReview.getTaskDefinitionKey(), resolved)
                .changeState();

        assertThat(singleTask(processInstanceId).getTaskDefinitionKey()).isEqualTo("risk_specialist_review");
    }

    @Test
    void branchGatewayTargetWithoutUpstreamCompletedTaskFails() {
        deployGatewayReviewProcess();
        String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY,
                Map.of("needInitialReview", false)).getId();
        Task supervisorReview = singleTask(processInstanceId);

        assertThatThrownBy(() -> WorkflowReturnTargetSupport.resolveTarget(
                processEngine.getHistoryService(), repositoryService, supervisorReview, "risk_review_type_branch"))
                .hasMessageContaining("上游没有可退回的已完成用户任务");
    }

    @Test
    void movingToMergeGatewayItselfFallsThroughBackToSourceNode() {
        deployGatewayReviewProcess();
        String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY,
                Map.of("needInitialReview", true)).getId();
        taskService.complete(singleTask(processInstanceId).getId());

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo("risk_supervisor_review", "risk_review_type_branch_merge")
                .changeState();

        assertThat(singleTask(processInstanceId).getTaskDefinitionKey()).isEqualTo("risk_supervisor_review");
    }

    private Task singleTask(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        assertThat(tasks).hasSize(1);
        return tasks.get(0);
    }

    private void deployGatewayReviewProcess() {
        repositoryService.createDeployment()
                .addBpmnModel(PROCESS_KEY + ".bpmn20.xml",
                        converter.toModel(designerJson(), PROCESS_KEY, "风控审核"))
                .deploy();
    }

    private String designerJson() {
        return """
                {
                  "id": "root",
                  "nodeType": "ROOT",
                  "nodeName": "发起",
                  "childNode": {
                    "id": "risk_review_type_branch",
                    "nodeType": "EXCLUSIVE_GATEWAY",
                    "nodeName": "是否需要初审",
                    "conditionNodes": [
                      {
                        "id": "initial_review_branch",
                        "nodeType": "EXCLUSIVE_BRANCH",
                        "conditionExpression": "${needInitialReview == true}",
                        "childNode": {
                          "id": "risk_specialist_review",
                          "nodeType": "APPROVAL",
                          "nodeName": "初审",
                          "properties": {"approvalConfig": {"assigneeIds": ["1001"]}}
                        }
                      },
                      {
                        "id": "initial_review_skip",
                        "nodeType": "EXCLUSIVE_BRANCH",
                        "nodeName": "无需初审"
                      }
                    ],
                    "childNode": {
                      "id": "risk_supervisor_review",
                      "nodeType": "APPROVAL",
                      "nodeName": "复审",
                      "properties": {"approvalConfig": {"assigneeIds": ["1002"]}},
                      "childNode": {
                        "id": "risk_final_review_branch",
                        "nodeType": "EXCLUSIVE_GATEWAY",
                        "nodeName": "是否需要终审",
                        "conditionNodes": [
                          {
                            "id": "final_review_branch",
                            "nodeType": "EXCLUSIVE_BRANCH",
                            "conditionExpression": "${needFinalReview == true}",
                            "childNode": {
                              "id": "risk_manager_review",
                              "nodeType": "APPROVAL",
                              "nodeName": "终审",
                              "properties": {"approvalConfig": {"assigneeIds": ["1003"]}}
                            }
                          },
                          {
                            "id": "final_review_skip",
                            "nodeType": "EXCLUSIVE_BRANCH",
                            "nodeName": "无需终审"
                          }
                        ]
                      }
                    }
                  }
                }
                """;
    }
}
