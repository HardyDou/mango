package io.mango.workflow.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.workflow.api.command.WithdrawWorkflowProcessCommand;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowInstanceStatus;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.core.entity.WorkflowFormInstanceEntity;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowProcessWithdrawalTest {

    private final WorkflowDefinitionMapper definitionMapper = mapperProxy(WorkflowDefinitionMapper.class);
    private final FormInstanceMapperStub formInstanceMapperStub = new FormInstanceMapperStub();
    private final WorkflowFormInstanceMapper formInstanceMapper = formInstanceMapperStub.mapper();
    private final WorkflowTaskRecordMapper taskRecordMapper = mapperProxy(WorkflowTaskRecordMapper.class);
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private TaskService taskService;
    @Mock
    private HistoryService historyService;
    @Mock
    private IWorkflowTaskRuntimeService workflowTaskRuntimeService;
    @Mock
    private IWorkflowBusinessApplyService workflowBusinessApplyService;
    @Mock
    private WorkflowEventPublisher workflowEventPublisher;

    private WorkflowProcessService service;

    @BeforeEach
    void setUp() {
        MangoContextHolder.clear();
        formInstanceMapperStub.reset();
        service = new WorkflowProcessService(
                definitionMapper,
                formInstanceMapper,
                taskRecordMapper,
                runtimeService,
                taskService,
                historyService,
                new ObjectMapper(),
                workflowTaskRuntimeService,
                workflowBusinessApplyService,
                workflowEventPublisher);
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void withdraw_missingTarget_rejectsBeforeReadingRuntime() {
        WithdrawWorkflowProcessCommand command = command(null, null, "申请人撤回");

        assertThatThrownBy(() -> service.withdraw(command))
                .hasMessageContaining("业务申请ID和流程实例ID不能同时为空");

        verifyNoInteractions(workflowBusinessApplyService, runtimeService, workflowEventPublisher);
    }

    @Test
    void withdraw_blankReason_rejectsBeforeReadingTarget() {
        useApplicantContext();
        WithdrawWorkflowProcessCommand command = command(1001L, null, "  ");

        assertThatThrownBy(() -> service.withdraw(command))
                .hasMessageContaining("撤回原因不能为空");

        verifyNoInteractions(workflowBusinessApplyService, runtimeService, workflowEventPublisher);
    }

    @Test
    void withdraw_missingTenantContext_rejectsBeforeTargetLookup() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, null, "applicant", "default", "USER", "USER", 1L, "internal-admin"));

        assertThatThrownBy(() -> service.withdraw(command(1001L, null, "申请人撤回")))
                .hasMessageContaining("缺少当前租户上下文");

        verifyNoInteractions(workflowBusinessApplyService, runtimeService, workflowEventPublisher);
    }

    @Test
    void withdraw_missingUserContext_rejectsBeforeTargetLookup() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        assertThatThrownBy(() -> service.withdraw(command(1001L, null, "申请人撤回")))
                .hasMessageContaining("缺少当前用户上下文");

        verifyNoInteractions(workflowBusinessApplyService, runtimeService, workflowEventPublisher);
    }

    @Test
    void withdraw_unknownApply_rejectsWithoutRuntimeSideEffects() {
        useApplicantContext();
        when(workflowBusinessApplyService.lockWithdrawalTarget(1001L, null)).thenReturn(null);

        assertThatThrownBy(() -> service.withdraw(command(1001L, null, "申请人撤回")))
                .hasMessageContaining("业务申请不存在");

        verifyNoInteractions(runtimeService, workflowEventPublisher);
    }

    @Test
    void withdraw_otherApplicantsApply_rejectsWithoutRuntimeSideEffects() {
        useApplicantContext();
        WorkflowBusinessApplyVO apply = apply(WorkflowApplyStatus.IN_APPROVAL);
        apply.setApplicantId(2002L);
        when(workflowBusinessApplyService.lockWithdrawalTarget(1001L, null)).thenReturn(apply);

        assertThatThrownBy(() -> service.withdraw(command(1001L, null, "申请人撤回")))
                .hasMessageContaining("当前用户无权撤回该流程");

        verifyNoInteractions(runtimeService, workflowEventPublisher);
        verify(workflowBusinessApplyService, never()).markWithdrawn(any(), any());
    }

    @Test
    void withdraw_mismatchedApplyAndProcessIdentifiers_rejectsClearly() {
        useApplicantContext();
        WorkflowBusinessApplyVO apply = apply(WorkflowApplyStatus.IN_APPROVAL);
        when(workflowBusinessApplyService.lockWithdrawalTarget(1001L, "proc-other")).thenReturn(apply);

        assertThatThrownBy(() -> service.withdraw(command(1001L, "proc-other", "申请人撤回")))
                .hasMessageContaining("业务申请ID与流程实例ID不匹配");

        verifyNoInteractions(runtimeService, workflowEventPublisher);
    }

    @ParameterizedTest
    @EnumSource(value = WorkflowApplyStatus.class, names = {
            "DRAFT", "SUBMITTED", "APPROVED", "REJECTED", "CANCELED", "TERMINATED"
    })
    void withdraw_nonRunningStatuses_returnExplicitBusinessFailure(WorkflowApplyStatus status) {
        useApplicantContext();
        when(workflowBusinessApplyService.lockWithdrawalTarget(1001L, null)).thenReturn(apply(status));

        assertThatThrownBy(() -> service.withdraw(command(1001L, null, "申请人撤回")))
                .hasMessageContaining("当前申请状态为" + status.getLabel() + "，不能撤回");

        verifyNoInteractions(runtimeService, workflowEventPublisher);
        verify(workflowBusinessApplyService, never()).markWithdrawn(any(), any());
    }

    @Test
    void withdraw_alreadyWithdrawn_returnsIdempotentSuccessWithoutDuplicateSideEffects() {
        useApplicantContext();
        WorkflowBusinessApplyVO apply = apply(WorkflowApplyStatus.WITHDRAWN);
        when(workflowBusinessApplyService.lockWithdrawalTarget(1001L, null)).thenReturn(apply);

        var result = service.withdraw(command(1001L, null, "重复请求"));

        assertThat(result.getApplyId()).isEqualTo(1001L);
        assertThat(result.getProcessInstanceId()).isEqualTo("proc-1");
        assertThat(result.getPreviousStatus()).isEqualTo(WorkflowApplyStatus.WITHDRAWN);
        assertThat(result.getApplyStatus()).isEqualTo(WorkflowApplyStatus.WITHDRAWN);
        assertThat(result.getWithdrawn()).isTrue();
        assertThat(result.getIdempotent()).isTrue();
        assertThat(result.getEnded()).isTrue();
        verifyNoInteractions(runtimeService, workflowEventPublisher);
        assertThat(formInstanceMapperStub.interactionCount()).isZero();
        verify(workflowBusinessApplyService, never()).markWithdrawn(any(), any());
    }

    @Test
    void withdraw_missingRunningInstance_doesNotCreatePartialWithdrawState() {
        useApplicantContext();
        when(workflowBusinessApplyService.lockWithdrawalTarget(1001L, null))
                .thenReturn(apply(WorkflowApplyStatus.IN_APPROVAL));
        ProcessInstanceQuery query = processQuery(null);

        assertThatThrownBy(() -> service.withdraw(command(1001L, null, "申请人撤回")))
                .hasMessageContaining("运行中的流程实例不存在");

        verify(query).singleResult();
        verifyNoInteractions(workflowEventPublisher);
        assertThat(formInstanceMapperStub.interactionCount()).isZero();
        verify(workflowBusinessApplyService, never()).markWithdrawn(any(), any());
    }

    @Test
    void withdraw_byApplyId_terminatesRuntimePersistsStateAndPublishesOrderedEvents() {
        useApplicantContext();
        WorkflowBusinessApplyVO apply = apply(WorkflowApplyStatus.IN_APPROVAL);
        when(workflowBusinessApplyService.lockWithdrawalTarget(1001L, null)).thenReturn(apply);
        processQuery(mock(ProcessInstance.class));
        WorkflowFormInstanceEntity formInstance = formInstance();
        formInstanceMapperStub.setSelected(formInstance);
        WorkflowBusinessApplyVO withdrawn = apply(WorkflowApplyStatus.WITHDRAWN);
        List<String> actions = new ArrayList<>();
        formInstanceMapperStub.recordActionsIn(actions);
        doAnswer(invocation -> {
            actions.add("delete-runtime");
            return null;
        }).when(runtimeService).deleteProcessInstance("proc-1", "资料有误，重新提交");
        when(workflowBusinessApplyService.markWithdrawn("proc-1", "资料有误，重新提交"))
                .thenAnswer(invocation -> {
                    actions.add("mark-apply");
                    return withdrawn;
                });
        doAnswer(invocation -> {
            actions.add("publish-withdrawn");
            return null;
        }).when(workflowEventPublisher).publishProcessWithdrawn(
                eq("proc-1"), same(formInstance), any(Map.class), eq("资料有误，重新提交"), same(withdrawn));
        doAnswer(invocation -> {
            actions.add("publish-ended");
            return null;
        }).when(workflowEventPublisher).publishProcessEnded(
                eq("proc-1"), same(formInstance), any(Map.class), eq("资料有误，重新提交"), same(withdrawn));

        var result = service.withdraw(command(1001L, null, " 资料有误，重新提交 "));

        assertSuccessfulResult(result);
        assertThat(formInstance.getStatus()).isEqualTo(WorkflowInstanceStatus.WITHDRAWN.name());
        assertThat(formInstance.getVariablesJson()).contains("\"businessType\":\"GUARANTEE_REVIEW\"")
                .contains("\"applyId\":\"1001\"");
        assertThat(formInstanceMapperStub.updated()).isSameAs(formInstance);
        assertThat(actions).containsExactly(
                "delete-runtime", "update-form", "mark-apply", "publish-withdrawn", "publish-ended");
    }

    @Test
    void withdraw_byProcessInstanceId_usesRuntimeVariablesWhenFormSnapshotIsMissing() {
        useApplicantContext();
        WorkflowBusinessApplyVO apply = apply(WorkflowApplyStatus.IN_APPROVAL);
        when(workflowBusinessApplyService.lockWithdrawalTarget(null, "proc-1")).thenReturn(apply);
        processQuery(mock(ProcessInstance.class));
        when(runtimeService.getVariables("proc-1")).thenReturn(Map.of("amount", 5000));
        WorkflowBusinessApplyVO withdrawn = apply(WorkflowApplyStatus.WITHDRAWN);
        when(workflowBusinessApplyService.markWithdrawn("proc-1", "业务申请取消")).thenReturn(withdrawn);

        var result = service.withdraw(command(null, " proc-1 ", "业务申请取消"));

        assertSuccessfulResult(result);
        assertThat(formInstanceMapperStub.updated()).isNull();
        verify(workflowEventPublisher).publishProcessWithdrawn(
                eq("proc-1"), isNull(), any(Map.class), eq("业务申请取消"), same(withdrawn));
        verify(workflowEventPublisher).publishProcessEnded(
                eq("proc-1"), isNull(), any(Map.class), eq("业务申请取消"), same(withdrawn));
    }

    private ProcessInstanceQuery processQuery(ProcessInstance instance) {
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId("proc-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(instance);
        return query;
    }

    private WorkflowFormInstanceEntity formInstance() {
        WorkflowFormInstanceEntity formInstance = new WorkflowFormInstanceEntity();
        formInstance.setId(2001L);
        formInstance.setProcessInstanceId("proc-1");
        formInstance.setBusinessKey("GUARANTEE-1");
        formInstance.setVariablesJson("{\"businessType\":\"GUARANTEE_REVIEW\",\"applyId\":\"1001\"}");
        formInstance.setStatus(WorkflowInstanceStatus.RUNNING.name());
        return formInstance;
    }

    private WorkflowBusinessApplyVO apply(WorkflowApplyStatus status) {
        WorkflowBusinessApplyVO apply = new WorkflowBusinessApplyVO();
        apply.setId(1001L);
        apply.setApplicantId(1001L);
        apply.setApplicantName("applicant");
        apply.setBusinessType("GUARANTEE_REVIEW");
        apply.setBusinessKey("GUARANTEE-1");
        apply.setProcessInstanceId("proc-1");
        apply.setApplyStatus(status);
        apply.setApplyStatusName(status.getLabel());
        return apply;
    }

    private WithdrawWorkflowProcessCommand command(Long applyId, String processInstanceId, String reason) {
        WithdrawWorkflowProcessCommand command = new WithdrawWorkflowProcessCommand();
        command.setApplyId(applyId);
        command.setProcessInstanceId(processInstanceId);
        command.setReason(reason);
        return command;
    }

    private void useApplicantContext() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "applicant", "default", "USER", "USER", 1L, "internal-admin"));
    }

    private void assertSuccessfulResult(io.mango.workflow.api.vo.WorkflowProcessWithdrawResultVO result) {
        assertThat(result.getApplyId()).isEqualTo(1001L);
        assertThat(result.getProcessInstanceId()).isEqualTo("proc-1");
        assertThat(result.getPreviousStatus()).isEqualTo(WorkflowApplyStatus.IN_APPROVAL);
        assertThat(result.getApplyStatus()).isEqualTo(WorkflowApplyStatus.WITHDRAWN);
        assertThat(result.getApplyStatusName()).isEqualTo("已撤回");
        assertThat(result.getWithdrawn()).isTrue();
        assertThat(result.getIdempotent()).isFalse();
        assertThat(result.getEnded()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapperProxy(Class<T> mapperType) {
        return (T) Proxy.newProxyInstance(
                mapperType.getClassLoader(),
                new Class<?>[]{mapperType},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }

    private static final class FormInstanceMapperStub {

        private WorkflowFormInstanceEntity selected;
        private WorkflowFormInstanceEntity updated;
        private List<String> actions;
        private int interactionCount;

        private WorkflowFormInstanceMapper mapper() {
            return (WorkflowFormInstanceMapper) Proxy.newProxyInstance(
                    WorkflowFormInstanceMapper.class.getClassLoader(),
                    new Class<?>[]{WorkflowFormInstanceMapper.class},
                    (proxy, method, args) -> {
                        interactionCount++;
                        if ("selectOne".equals(method.getName())) {
                            return selected;
                        }
                        if ("updateById".equals(method.getName())
                                && args != null
                                && args.length == 1
                                && args[0] instanceof WorkflowFormInstanceEntity formInstance) {
                            updated = formInstance;
                            if (actions != null) {
                                actions.add("update-form");
                            }
                            return 1;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private void reset() {
            selected = null;
            updated = null;
            actions = null;
            interactionCount = 0;
        }

        private void setSelected(WorkflowFormInstanceEntity selected) {
            this.selected = selected;
        }

        private void recordActionsIn(List<String> actions) {
            this.actions = actions;
        }

        private WorkflowFormInstanceEntity updated() {
            return updated;
        }

        private int interactionCount() {
            return interactionCount;
        }
    }
}
