package io.mango.workflow.starter.controller;

import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.command.StartBusinessWorkflowCommand;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowStartResultVO;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowProcessService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 节点亲和亲和键的 HTTP 层契约：入参绑定、长度校验和响应回显。 */
class WorkflowAffinityKeyHttpContractTest {

    private static final String LONG_AFFINITY_KEY = "P".repeat(129);

    private final IWorkflowBusinessApplyService businessApplyService = mock(IWorkflowBusinessApplyService.class);
    private final IWorkflowProcessService processService = mock(IWorkflowProcessService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new WorkflowBusinessApplyController(businessApplyService),
            new WorkflowProcessController(processService)).build();

    @Test
    void createApplyBindsAffinityKeyAndEchoesItInResponse() throws Exception {
        when(businessApplyService.create(any())).thenAnswer(invocation -> {
            CreateWorkflowBusinessApplyCommand command = invocation.getArgument(0);
            WorkflowBusinessApplyVO vo = new WorkflowBusinessApplyVO();
            vo.setId(1001L);
            vo.setBusinessType(command.getBusinessType());
            vo.setBusinessKey(command.getBusinessKey());
            vo.setAffinityKey(command.getAffinityKey());
            return vo;
        });

        String body = mockMvc.perform(post("/workflow/business-applies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessType":"BAOHAN_RC_PROJECT_REVIEW","businessKey":"BIZ-P1-V1",
                                 "affinityKey":"PROJECT-P1","applyTitle":"项目风控审批"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"affinityKey\":\"PROJECT-P1\"");

        ArgumentCaptor<CreateWorkflowBusinessApplyCommand> captor =
                ArgumentCaptor.forClass(CreateWorkflowBusinessApplyCommand.class);
        verify(businessApplyService).create(captor.capture());
        assertThat(captor.getValue().getAffinityKey()).isEqualTo("PROJECT-P1");
    }

    @Test
    void startBusinessWorkflowBindsAffinityKeyIntoApplyCommand() throws Exception {
        when(processService.startBusinessWorkflow(any())).thenReturn(new WorkflowStartResultVO());

        mockMvc.perform(post("/workflow/processes/start-business")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"definitionId":1001,"businessType":"BAOHAN_RC_PROJECT_REVIEW",
                                 "businessKey":"BIZ-P1-V2","affinityKey":"PROJECT-P1","applyTitle":"项目风控审批"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<StartBusinessWorkflowCommand> captor =
                ArgumentCaptor.forClass(StartBusinessWorkflowCommand.class);
        verify(processService).startBusinessWorkflow(captor.capture());
        assertThat(captor.getValue().getAffinityKey()).isEqualTo("PROJECT-P1");
    }

    @Test
    void affinityKeyLongerThan128CharactersIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/workflow/business-applies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessType":"BAOHAN_RC_PROJECT_REVIEW","businessKey":"BIZ-P1-V1",
                                 "affinityKey":"%s","applyTitle":"项目风控审批"}
                                """.formatted(LONG_AFFINITY_KEY)))
                .andExpect(status().isBadRequest());

        verify(businessApplyService, never()).create(any());
    }
}
