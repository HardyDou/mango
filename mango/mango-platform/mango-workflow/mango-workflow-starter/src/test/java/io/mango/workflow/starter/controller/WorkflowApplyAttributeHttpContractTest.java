package io.mango.workflow.starter.controller;

import io.mango.workflow.api.command.UpdateWorkflowBusinessApplyFilterAttributesCommand;
import io.mango.workflow.api.query.WorkflowTaskBusinessPageQuery;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowTaskBusinessQueryService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 业务申请可筛属性的 HTTP 层契约：请求绑定、嵌套校验和更新入口。
 */
class WorkflowApplyAttributeHttpContractTest {

    private final IWorkflowBusinessApplyService businessApplyService = mock(IWorkflowBusinessApplyService.class);
    private final IWorkflowTaskBusinessQueryService businessQueryService = mock(IWorkflowTaskBusinessQueryService.class);
    private final IWorkflowTaskRuntimeService taskRuntimeService = mock(IWorkflowTaskRuntimeService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new WorkflowBusinessApplyController(businessApplyService),
            new WorkflowTaskController(taskRuntimeService, businessQueryService)).build();

    @Test
    void taskPageBindsAttributeFiltersApplyTimeAndSort() throws Exception {
        mockMvc.perform(post("/workflow/tasks/page-by-business-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessType":"BAOHAN_RC_PROJECT_REVIEW",
                                 "applyAttributeFilters":[
                                   {"key":"guaranteeTypeCode","values":["PERF","BID"]},
                                   {"key":"beneficiary","values":["中铁三局"]}],
                                 "applyTimeBegin":"2026-01-01T00:00:00",
                                 "applyTimeEnd":"2026-03-01T00:00:00",
                                 "sortBy":"applyTime",
                                 "page":1,
                                 "size":20}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<WorkflowTaskBusinessPageQuery> captor =
                ArgumentCaptor.forClass(WorkflowTaskBusinessPageQuery.class);
        verify(businessQueryService).pageByBusinessKey(captor.capture());
        WorkflowTaskBusinessPageQuery query = captor.getValue();
        assertThat(query.getApplyAttributeFilters()).hasSize(2);
        assertThat(query.getApplyAttributeFilters().getFirst().getKey()).isEqualTo("guaranteeTypeCode");
        assertThat(query.getApplyAttributeFilters().getFirst().getValues()).containsExactly("PERF", "BID");
        assertThat(query.getApplyAttributeFilters().get(1).getKey()).isEqualTo("beneficiary");
        assertThat(query.getApplyTimeBegin()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(query.getApplyTimeEnd()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
        assertThat(query.getSortBy()).isEqualTo("applyTime");
        assertThat(query.getSize()).isEqualTo(20);
    }

    @Test
    void taskPageRejectsInvalidAttributeKeyAndTooManyConditions() throws Exception {
        mockMvc.perform(post("/workflow/tasks/page-by-business-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessType":"BAOHAN_RC_PROJECT_REVIEW",
                                 "applyAttributeFilters":[{"key":"1invalid","values":["PERF"]}]}
                                """))
                .andExpect(status().isBadRequest());

        StringBuilder tooMany = new StringBuilder();
        for (int index = 0; index < 11; index++) {
            tooMany.append(index == 0 ? "" : ",")
                    .append("{\"key\":\"attr").append(index).append("\",\"values\":[\"v\"]}");
        }
        mockMvc.perform(post("/workflow/tasks/page-by-business-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessType":"BAOHAN_RC_PROJECT_REVIEW","applyAttributeFilters":[%s]}
                                """.formatted(tooMany)))
                .andExpect(status().isBadRequest());

        verify(businessQueryService, never()).pageByBusinessKey(any());
    }

    @Test
    void updateFilterAttributesBindsCommandAndAllowsClearing() throws Exception {
        mockMvc.perform(put("/workflow/business-applies/filter-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applyId":9001,
                                 "filterAttributes":[
                                   {"key":"versionStatus","values":["REVIEWED"]},
                                   {"key":"beneficiary","values":["中铁三局","中铁四局"]}]}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateWorkflowBusinessApplyFilterAttributesCommand> captor =
                ArgumentCaptor.forClass(UpdateWorkflowBusinessApplyFilterAttributesCommand.class);
        verify(businessApplyService).updateFilterAttributes(captor.capture());
        assertThat(captor.getValue().getApplyId()).isEqualTo(9001L);
        assertThat(captor.getValue().getFilterAttributes()).hasSize(2);
        assertThat(captor.getValue().getFilterAttributes().getFirst().getKey()).isEqualTo("versionStatus");
        assertThat(captor.getValue().getFilterAttributes().get(1).getValues())
                .containsExactly("中铁三局", "中铁四局");

        mockMvc.perform(put("/workflow/business-applies/filter-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applyId\":9002,\"filterAttributes\":[]}"))
                .andExpect(status().isOk());
        verify(businessApplyService, times(2)).updateFilterAttributes(any());
    }

    @Test
    void updateFilterAttributesRejectsMissingApplyId() throws Exception {
        mockMvc.perform(put("/workflow/business-applies/filter-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filterAttributes\":[]}"))
                .andExpect(status().isBadRequest());

        verify(businessApplyService, never()).updateFilterAttributes(any());
    }
}
