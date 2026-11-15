package io.mango.workflow.starter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.enums.WorkflowTaskAssigneeSource;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 业务申请列表当前任务的释放资格字段 HTTP 契约（Issue #1282）。
 *
 * <p>业务列表是业务端判定释放按钮的唯一入口：本测试锁定返回 JSON 中 {@code unclaimable}、
 * {@code assignSource}、{@code claimTime} 的字段名、取值与空值语义，避免业务端退回逐行查询任务详情。
 */
class WorkflowBusinessApplyCurrentTaskHttpContractTest {

    private final IWorkflowBusinessApplyService businessApplyService = mock(IWorkflowBusinessApplyService.class);

    /** 与运行态一致的时间序列化：ISO-8601 字符串，而不是 epoch 数组。 */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new WorkflowBusinessApplyController(businessApplyService))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();

    @Test
    void pageReturnsReleaseEligibilityFieldsForAssignedCurrentTask() throws Exception {
        WorkflowBusinessApplyCurrentTaskVO assigned = currentTask("auto_review", "finance",
                WorkflowTaskClaimStatus.ASSIGNED, WorkflowTaskAssigneeSource.AUTO_ASSIGN,
                LocalDateTime.of(2026, 11, 15, 10, 15, 30));
        assigned.setUnclaimable(true);
        when(businessApplyService.page(any())).thenReturn(pageOf(assigned));

        JsonNode currentTask = currentTaskOfPageResponse();

        assertThat(currentTask.path("claimStatus").asText()).isEqualTo("ASSIGNED");
        assertThat(currentTask.path("unclaimable").asBoolean()).isTrue();
        assertThat(currentTask.path("assignSource").asText()).isEqualTo("AUTO_ASSIGN");
        assertThat(currentTask.path("claimTime").asText()).isEqualTo("2026-11-15T10:15:30");
    }

    @Test
    void pageKeepsReleaseFieldsEmptyForTaskInCandidatePool() throws Exception {
        WorkflowBusinessApplyCurrentTaskVO pooled = currentTask("role_review", null,
                WorkflowTaskClaimStatus.UNCLAIMED, null, null);
        pooled.setUnclaimable(false);
        when(businessApplyService.page(any())).thenReturn(pageOf(pooled));

        JsonNode currentTask = currentTaskOfPageResponse();

        assertThat(currentTask.path("claimStatus").asText()).isEqualTo("UNCLAIMED");
        assertThat(currentTask.path("unclaimable").asBoolean()).isFalse();
        // 候选池任务没有承办人：来源与领取时间必须为空（序列化为 null 或省略，二者对业务端等价）。
        assertThat(currentTask.hasNonNull("assignSource")).isFalse();
        assertThat(currentTask.hasNonNull("claimTime")).isFalse();
    }

    private JsonNode currentTaskOfPageResponse() throws Exception {
        String body = mockMvc.perform(post("/workflow/business-applies/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":1,\"size\":20}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        assertThat(response.path("success").asBoolean()).isTrue();
        return response.path("data").path("list").get(0).path("currentTasks").get(0);
    }

    private WorkflowBusinessApplyCurrentTaskVO currentTask(String taskDefinitionKey, String assigneeName,
                                                            WorkflowTaskClaimStatus claimStatus,
                                                            WorkflowTaskAssigneeSource assignSource,
                                                            LocalDateTime claimTime) {
        WorkflowBusinessApplyCurrentTaskVO currentTask = new WorkflowBusinessApplyCurrentTaskVO();
        currentTask.setTaskId("task-" + taskDefinitionKey);
        currentTask.setTaskDefinitionKey(taskDefinitionKey);
        currentTask.setTaskName("审批");
        currentTask.setAssigneeName(assigneeName);
        currentTask.setClaimStatus(claimStatus);
        currentTask.setArrivedAt(LocalDateTime.of(2026, 11, 15, 9, 0, 0));
        currentTask.setAssignSource(assignSource);
        currentTask.setClaimTime(claimTime);
        return currentTask;
    }

    private PageResult<WorkflowBusinessApplyVO> pageOf(WorkflowBusinessApplyCurrentTaskVO currentTask) {
        WorkflowBusinessApplyVO apply = new WorkflowBusinessApplyVO();
        apply.setCurrentTasks(List.of(currentTask));
        PageResult<WorkflowBusinessApplyVO> page = new PageResult<>();
        page.setList(List.of(apply));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        page.setPages(1);
        return page;
    }
}
