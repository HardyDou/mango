package io.mango.workflow.starter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.enums.WorkflowTaskAssigneeSource;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.request.WorkflowTaskBusinessPageRequest;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.service.IWorkflowTaskBusinessQueryService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 任务释放事实字段的 HTTP 契约（Issue #1286）。
 *
 * <p>平台只回答两件事：{@code assigneeName} 是谁的任务、{@code releasable} 这个任务此刻能不能交回候选池；
 * 不能释放时 {@code releaseBlockedReason} 给出任务侧原因。谁能发起释放由调用方的功能权限决定，
 * 因此本测试锁定「是否可释放与调用者无关」这一契约：同一份任务数据在 {@code releasable} 上不随查看人变化。
 */
class WorkflowTaskReleaseFactsHttpContractTest {

    private final IWorkflowTaskRuntimeService runtimeService = mock(IWorkflowTaskRuntimeService.class);
    private final IWorkflowTaskBusinessQueryService businessQueryService =
            mock(IWorkflowTaskBusinessQueryService.class);

    /** 与运行态一致的时间序列化：ISO-8601 字符串，而不是 epoch 数组。 */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new WorkflowTaskController(runtimeService, businessQueryService))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();

    @Test
    void businessPageExposesReleasableFactsForPlatformDispatchedTask() throws Exception {
        WorkflowTaskVO dispatched = task("T1", "admin", WorkflowTaskClaimStatus.ASSIGNED,
                WorkflowTaskAssigneeSource.AUTO_ASSIGN, LocalDateTime.of(2026, 11, 15, 10, 15, 30));
        dispatched.setReleasable(Boolean.TRUE);
        dispatched.setUnclaimable(Boolean.FALSE);
        when(businessQueryService.pageByBusinessKey(any(WorkflowTaskBusinessPageRequest.class)))
                .thenReturn(pageOf(dispatched));

        JsonNode task = firstTaskOfBusinessPage();

        assertThat(task.path("assigneeName").asText()).isEqualTo("admin");
        assertThat(task.path("assignSource").asText()).isEqualTo("AUTO_ASSIGN");
        assertThat(task.path("claimStatus").asText()).isEqualTo("ASSIGNED");
        assertThat(task.path("releasable").asBoolean()).isTrue();
        assertThat(task.hasNonNull("releaseBlockedReason")).isFalse();
        // 查看人不是承办人时，旧字段 unclaimable 为 false，但 releasable 仍为 true。
        assertThat(task.path("unclaimable").asBoolean()).isFalse();
        assertThat(task.path("claimTime").asText()).isEqualTo("2026-11-15T10:15:30");
    }

    @Test
    void businessPageExplainsWhyTaskCannotBeReleased() throws Exception {
        WorkflowTaskVO transferred = task("T2", "finance", WorkflowTaskClaimStatus.ASSIGNED,
                WorkflowTaskAssigneeSource.TRANSFER, LocalDateTime.of(2026, 11, 15, 11, 0));
        transferred.setReleasable(Boolean.FALSE);
        transferred.setReleaseBlockedReason(WorkflowTaskAssigneeSource.TRANSFER.getReleaseBlockedReason());
        transferred.setUnclaimable(Boolean.FALSE);
        when(businessQueryService.pageByBusinessKey(any(WorkflowTaskBusinessPageRequest.class)))
                .thenReturn(pageOf(transferred));

        JsonNode task = firstTaskOfBusinessPage();

        assertThat(task.path("releasable").asBoolean()).isFalse();
        assertThat(task.path("releaseBlockedReason").asText())
                .isEqualTo(WorkflowTaskAssigneeSource.TRANSFER.getReleaseBlockedReason());
    }

    @Test
    void businessPageMarksUnclaimedTaskWithoutCandidatesAsNone() throws Exception {
        WorkflowTaskVO pooled = task("T3", null, WorkflowTaskClaimStatus.NONE, null, null);
        pooled.setReleasable(Boolean.FALSE);
        pooled.setClaimable(Boolean.FALSE);
        when(businessQueryService.pageByBusinessKey(any(WorkflowTaskBusinessPageRequest.class)))
                .thenReturn(pageOf(pooled));

        JsonNode task = firstTaskOfBusinessPage();

        assertThat(task.path("claimStatus").asText()).isEqualTo("NONE");
        assertThat(task.path("releasable").asBoolean()).isFalse();
        assertThat(task.hasNonNull("assigneeName")).isFalse();
        assertThat(task.hasNonNull("releaseBlockedReason")).isFalse();
    }

    @Test
    void detailExposesReleasableOnTaskView() throws Exception {
        WorkflowTaskVO detailTask = task("T4", "admin", WorkflowTaskClaimStatus.ASSIGNED,
                WorkflowTaskAssigneeSource.AUTO_ASSIGN, LocalDateTime.of(2026, 11, 15, 12, 0));
        detailTask.setReleasable(Boolean.TRUE);
        WorkflowTaskDetailVO detail = new WorkflowTaskDetailVO();
        detail.setTask(detailTask);
        when(runtimeService.detail("T4")).thenReturn(detail);

        String body = mockMvc.perform(get("/workflow/tasks/detail").param("taskId", "T4"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode task = objectMapper.readTree(body).path("data").path("task");
        assertThat(task.path("releasable").asBoolean()).isTrue();
        assertThat(task.path("assignSource").asText()).isEqualTo("AUTO_ASSIGN");
        assertThat(task.hasNonNull("releaseBlockedReason")).isFalse();
    }

    private JsonNode firstTaskOfBusinessPage() throws Exception {
        String body = mockMvc.perform(post("/workflow/tasks/page-by-business-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessType\":\"BAOHAN_RC_PROJECT_REVIEW\",\"businessKeys\":[\"RC1\"]}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("list").get(0);
    }

    private PageResult<WorkflowTaskVO> pageOf(WorkflowTaskVO task) {
        PageResult<WorkflowTaskVO> page = new PageResult<>();
        page.setList(List.of(task));
        page.setTotal(1L);
        return page;
    }

    private WorkflowTaskVO task(String taskId, String assignee, WorkflowTaskClaimStatus claimStatus,
                                WorkflowTaskAssigneeSource assignSource, LocalDateTime claimTime) {
        WorkflowTaskVO task = new WorkflowTaskVO();
        task.setId(taskId);
        task.setTaskName("初审");
        task.setTaskDefinitionKey("FIRST_REVIEW");
        task.setProcessInstanceId("P1");
        task.setBusinessKey("RC1");
        task.setAssigneeName(assignee);
        task.setClaimStatus(claimStatus);
        task.setAssignSource(assignSource);
        task.setClaimTime(claimTime);
        return task;
    }
}
