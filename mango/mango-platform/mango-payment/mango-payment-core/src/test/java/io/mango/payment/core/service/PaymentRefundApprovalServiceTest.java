package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentRefundApprovalStatusEnum;
import io.mango.payment.api.command.CreatePaymentOpenRefundCommand;
import io.mango.payment.api.command.CreatePaymentRefundApprovalCommand;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundApprovalVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentRefundApprovalEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundApprovalMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.workflow.api.WorkflowBusinessApplyApi;
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRefundApprovalServiceTest {

    private PaymentRefundApprovalMapper refundApprovalMapper;
    private PaymentApplicationMapper applicationMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private PaymentRefundOrderMapper refundOrderMapper;
    private PaymentRefundApplyService refundApplyService;
    private PaymentOrderStateService orderStateService;
    private PaymentOperationAuditService auditService;
    private PaymentNumberService numberService;
    private WorkflowProcessApi workflowProcessApi;
    private WorkflowBusinessApplyApi workflowBusinessApplyApi;
    private PaymentRefundApprovalService service;

    @BeforeEach
    void setUp() {
        refundApprovalMapper = mock(PaymentRefundApprovalMapper.class);
        applicationMapper = mock(PaymentApplicationMapper.class);
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        refundOrderMapper = mock(PaymentRefundOrderMapper.class);
        refundApplyService = mock(PaymentRefundApplyService.class);
        orderStateService = new PaymentOrderStateService();
        auditService = mock(PaymentOperationAuditService.class);
        numberService = mock(PaymentNumberService.class);
        workflowProcessApi = mock(WorkflowProcessApi.class);
        workflowBusinessApplyApi = mock(WorkflowBusinessApplyApi.class);
        when(numberService.next(PaymentNumberService.PAY_BIZ_REFUND_NO)).thenReturn("BR2026060600000001");
        when(numberService.next(PaymentNumberService.PAY_REFUND_APPROVAL_NO)).thenReturn("RA2026060600000001");
        when(refundApprovalMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);
        when(refundApprovalMapper.updateById(any(PaymentRefundApprovalEntity.class))).thenReturn(1);
        service = new PaymentRefundApprovalService(
                refundApprovalMapper,
                applicationMapper,
                paymentOrderMapper,
                refundOrderMapper,
                refundApplyService,
                orderStateService,
                auditService,
                numberService,
                workflowProcessApi,
                workflowBusinessApplyApi,
                new TestTransactionManager());
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "applicant", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("createRefundApproval should include pending approvals in refundable validation")
    void createRefundApproval_pendingApprovalsOccupyRefundableAmount() {
        PaymentOrderVO paymentOrder = paymentOrder();
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(5000L);
        when(refundApprovalMapper.sumPendingApprovalAmount(1L, 370001L)).thenReturn(1000L);

        assertThatThrownBy(() -> service.createRefundApproval(createCommand()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED.getCode());
    }

    @Test
    @DisplayName("createRefundApproval should enter approval only after workflow starts")
    void createRefundApproval_workflowStarted_entersApproval() {
        PaymentOrderVO paymentOrder = paymentOrder();
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(0L);
        when(refundApprovalMapper.sumPendingApprovalAmount(1L, 370001L)).thenReturn(0L);
        when(workflowProcessApi.start(any())).thenReturn(R.ok(workflowProcess()));
        when(refundApprovalMapper.selectRefundApprovalDetail(1L, 390001L)).thenReturn(approvalDetail("IN_APPROVAL"));
        ArgumentCaptor<PaymentRefundApprovalEntity> approvalCaptor = ArgumentCaptor.forClass(PaymentRefundApprovalEntity.class);
        ArgumentCaptor<UpdateWrapper<PaymentRefundApprovalEntity>> updateCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
        when(refundApprovalMapper.insert(any(PaymentRefundApprovalEntity.class))).thenAnswer(invocation -> {
            PaymentRefundApprovalEntity entity = invocation.getArgument(0);
            entity.setId(390001L);
            return 1;
        });

        PaymentRefundApprovalVO result = service.createRefundApproval(createCommand());

        verify(refundApprovalMapper).insert(approvalCaptor.capture());
        assertThat(approvalCaptor.getValue().getStatus()).isEqualTo(PaymentRefundApprovalStatusEnum.PENDING.getCode());
        verify(refundApprovalMapper).update(eq(null), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getSqlSet()).contains("status");
        assertThat(updateCaptor.getValue().getParamNameValuePairs())
                .containsValue(PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode());
        assertThat(result.getStatus()).isEqualTo(PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode());
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CREATE_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                "RA2026060600000001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("createRefundApproval should keep in-approval status when success audit throws")
    void createRefundApproval_workflowStartedAndAuditFailed_keepsStartedStatus() {
        PaymentOrderVO paymentOrder = paymentOrder();
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(0L);
        when(refundApprovalMapper.sumPendingApprovalAmount(1L, 370001L)).thenReturn(0L);
        when(workflowProcessApi.start(any())).thenReturn(R.ok(workflowProcess()));
        when(refundApprovalMapper.selectRefundApprovalDetail(1L, 390001L)).thenReturn(approvalDetail("IN_APPROVAL"));
        doThrow(new IllegalStateException("审计不可用"))
                .when(auditService)
                .record(
                        PaymentOperationAuditService.ACTION_CREATE_REFUND_APPROVAL,
                        PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                        "RA2026060600000001",
                        PaymentOperationAuditService.RESULT_SUCCESS);
        when(refundApprovalMapper.insert(any(PaymentRefundApprovalEntity.class))).thenAnswer(invocation -> {
            PaymentRefundApprovalEntity entity = invocation.getArgument(0);
            entity.setId(390001L);
            return 1;
        });
        ArgumentCaptor<UpdateWrapper<PaymentRefundApprovalEntity>> updateCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);

        PaymentRefundApprovalVO result = service.createRefundApproval(createCommand());

        assertThat(result.getStatus()).isEqualTo(PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode());
        verify(refundApprovalMapper).update(eq(null), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getParamNameValuePairs())
                .containsValue(PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode());
    }

    @Test
    @DisplayName("createRefundApproval should mark local approval failed when workflow start fails")
    void createRefundApproval_workflowStartFailed_releasesApprovalOccupation() {
        PaymentOrderVO paymentOrder = paymentOrder();
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(0L);
        when(refundApprovalMapper.sumPendingApprovalAmount(1L, 370001L)).thenReturn(0L);
        when(workflowProcessApi.start(any())).thenReturn(R.fail(PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "工作流不可用"));
        ArgumentCaptor<PaymentRefundApprovalEntity> approvalCaptor = ArgumentCaptor.forClass(PaymentRefundApprovalEntity.class);
        ArgumentCaptor<UpdateWrapper<PaymentRefundApprovalEntity>> updateCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);

        assertThatThrownBy(() -> service.createRefundApproval(createCommand()))
                .isInstanceOf(BizException.class)
                .hasMessage("工作流不可用");

        verify(refundApprovalMapper).insert(approvalCaptor.capture());
        assertThat(approvalCaptor.getValue().getStatus()).isEqualTo(PaymentRefundApprovalStatusEnum.PENDING.getCode());
        verify(refundApprovalMapper).update(eq(null), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getParamNameValuePairs())
                .containsValue(PaymentRefundApprovalStatusEnum.WORKFLOW_START_FAILED.getCode());
        verify(refundApprovalMapper, never()).selectRefundApprovalDetail(any(), any());
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CREATE_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                "RA2026060600000001",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("createRefundApproval should preserve workflow exception when failure compensation fails")
    void createRefundApproval_workflowStartFailedAndCompensationFailed_preservesWorkflowException() {
        PaymentOrderVO paymentOrder = paymentOrder();
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(0L);
        when(refundApprovalMapper.sumPendingApprovalAmount(1L, 370001L)).thenReturn(0L);
        when(workflowProcessApi.start(any()))
                .thenReturn(R.fail(PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "工作流不可用"));
        when(refundApprovalMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> service.createRefundApproval(createCommand()))
                .isInstanceOf(BizException.class)
                .hasMessage("工作流不可用")
                .satisfies(ex -> assertThat(ex.getSuppressed())
                        .hasSize(1)
                        .extracting(Throwable::getMessage)
                        .containsExactly("退款审批工作流启动失败状态更新失败"));
    }

    @Test
    @DisplayName("createRefundApproval should keep failed status when failure audit throws")
    void createRefundApproval_workflowStartFailedAndAuditFailed_keepsFailedStatus() {
        PaymentOrderVO paymentOrder = paymentOrder();
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(0L);
        when(refundApprovalMapper.sumPendingApprovalAmount(1L, 370001L)).thenReturn(0L);
        when(workflowProcessApi.start(any()))
                .thenReturn(R.fail(PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "工作流不可用"));
        doThrow(new IllegalStateException("审计不可用"))
                .when(auditService)
                .record(
                        PaymentOperationAuditService.ACTION_CREATE_REFUND_APPROVAL,
                        PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                        "RA2026060600000001",
                        PaymentOperationAuditService.RESULT_REJECTED);
        ArgumentCaptor<UpdateWrapper<PaymentRefundApprovalEntity>> updateCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);

        assertThatThrownBy(() -> service.createRefundApproval(createCommand()))
                .isInstanceOf(BizException.class)
                .hasMessage("工作流不可用")
                .satisfies(ex -> assertThat(ex.getSuppressed())
                        .hasSize(1)
                        .extracting(Throwable::getMessage)
                        .containsExactly("审计不可用"));

        verify(refundApprovalMapper).update(eq(null), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getParamNameValuePairs())
                .containsValue(PaymentRefundApprovalStatusEnum.WORKFLOW_START_FAILED.getCode());
    }

    @Test
    @DisplayName("createRefundApproval should accept workflow synchronous completion before start returns")
    void createRefundApproval_workflowCompletedSynchronously_returnsFinalApproval() {
        PaymentOrderVO paymentOrder = paymentOrder();
        PaymentRefundApprovalEntity approved = approvalEntity(PaymentRefundApprovalStatusEnum.APPROVED.getCode());
        approved.setApprovalNo("RA2026060600000001");
        approved.setRefundOrderId(380001L);
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(0L);
        when(refundApprovalMapper.sumPendingApprovalAmount(1L, 370001L)).thenReturn(0L);
        when(workflowProcessApi.start(any())).thenReturn(R.ok(workflowProcess()));
        when(refundApprovalMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(0);
        when(refundApprovalMapper.selectEntityByApprovalNoForUpdate(1L, "RA2026060600000001")).thenReturn(approved);
        when(refundApprovalMapper.selectRefundApprovalDetail(1L, 390001L)).thenReturn(approvalDetail("APPROVED"));
        when(refundApprovalMapper.insert(any(PaymentRefundApprovalEntity.class))).thenAnswer(invocation -> {
            PaymentRefundApprovalEntity entity = invocation.getArgument(0);
            entity.setId(390001L);
            return 1;
        });

        PaymentRefundApprovalVO result = service.createRefundApproval(createCommand());

        assertThat(result.getStatus()).isEqualTo(PaymentRefundApprovalStatusEnum.APPROVED.getCode());
        assertThat(approved.getWorkflowProcessInstanceId()).isEqualTo("PROC-1");
        assertThat(approved.getWorkflowApplyId()).isEqualTo(390001L);
        verify(refundApprovalMapper).updateById(approved);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CREATE_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                "RA2026060600000001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("createRefundApproval should reuse workflow start failed approval for same biz refund number")
    void createRefundApproval_retryWorkflowStartFailedApproval_reusesExistingApproval() {
        PaymentOrderVO paymentOrder = paymentOrder();
        PaymentRefundApprovalEntity failed = approvalEntity(PaymentRefundApprovalStatusEnum.WORKFLOW_START_FAILED.getCode());
        failed.setApprovalNo("RA2026060600000001");
        failed.setReviewReason("工作流不可用");
        failed.setReviewTime(LocalDateTime.of(2026, 6, 7, 10, 5));
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundApprovalMapper.selectEntityByBizRefundNoForUpdate(1L, "app_openapi", "MANUAL-REFUND-001")).thenReturn(failed);
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(0L);
        when(refundApprovalMapper.sumPendingApprovalAmount(1L, 370001L)).thenReturn(0L);
        when(workflowProcessApi.start(any())).thenReturn(R.ok(workflowProcess()));
        when(refundApprovalMapper.selectRefundApprovalDetail(1L, 390001L)).thenReturn(approvalDetail("IN_APPROVAL"));

        PaymentRefundApprovalVO result = service.createRefundApproval(createCommand());

        assertThat(result.getStatus()).isEqualTo(PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode());
        assertThat(failed.getStatus()).isEqualTo(PaymentRefundApprovalStatusEnum.PENDING.getCode());
        assertThat(failed.getReviewReason()).isNull();
        assertThat(failed.getReviewTime()).isNull();
        verify(refundApprovalMapper, never()).insert(any(PaymentRefundApprovalEntity.class));
        verify(refundApprovalMapper).updateById(failed);
        verify(refundApprovalMapper).update(eq(null), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("approveByWorkflow should create refund through shared refund service")
    void approveByWorkflow_appliesRefundAndRecordsAudit() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                null, "1", "workflow", "INTERNAL", "SYSTEM", "SYSTEM", null, "internal-admin"));
        PaymentRefundApprovalEntity entity = approvalEntity("IN_APPROVAL");
        entity.setApplicantId(1001L);
        when(refundApprovalMapper.selectEntityByApprovalNoForUpdate(1L, "RFA202606070001")).thenReturn(entity);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        PaymentOpenRefundOrderVO refundOrder = new PaymentOpenRefundOrderVO();
        refundOrder.setId(380001L);
        refundOrder.setRefundOrderNo("RO202606070001");
        when(refundApplyService.applyRefund(any(), any(), eq(PaymentOrderStatusFlowService.SOURCE_MANUAL_REFUND_APPROVAL), eq("RFA202606070001"), eq(true)))
                .thenReturn(refundOrder);
        ArgumentCaptor<CreatePaymentOpenRefundCommand> refundCommandCaptor = ArgumentCaptor.forClass(CreatePaymentOpenRefundCommand.class);

        service.approveByWorkflow(1L, "RFA202606070001", "PROC-1");

        verify(refundApplyService).applyRefund(
                any(),
                refundCommandCaptor.capture(),
                eq(PaymentOrderStatusFlowService.SOURCE_MANUAL_REFUND_APPROVAL),
                eq("RFA202606070001"),
                eq(true));
        assertThat(refundCommandCaptor.getValue().getBizRefundNo()).isEqualTo("MANUAL-REFUND-001");
        assertThat(entity.getRefundOrderId()).isEqualTo(380001L);
        assertThat(entity.getStatus()).isEqualTo("APPROVED");
        verify(refundApprovalMapper, times(2)).updateById(entity);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_APPROVE_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                "RFA202606070001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("rejectByWorkflow should close approval and record audit")
    void rejectByWorkflow_recordsAudit() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                null, "1", "workflow", "INTERNAL", "SYSTEM", "SYSTEM", null, "internal-admin"));
        PaymentRefundApprovalEntity entity = approvalEntity("IN_APPROVAL");
        entity.setApplicantId(1001L);
        when(refundApprovalMapper.selectEntityByApprovalNoForUpdate(1L, "RFA202606070001")).thenReturn(entity);

        service.rejectByWorkflow(1L, "RFA202606070001", "PROC-1", "资料不完整");

        assertThat(entity.getStatus()).isEqualTo("REJECTED");
        assertThat(entity.getReviewerName()).isEqualTo("workflow");
        assertThat(entity.getReviewReason()).isEqualTo("资料不完整");
        verify(refundApprovalMapper).updateById(entity);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_REJECT_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                "RFA202606070001",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    private CreatePaymentRefundApprovalCommand createCommand() {
        CreatePaymentRefundApprovalCommand command = new CreatePaymentRefundApprovalCommand();
        command.setPaymentOrderId(370001L);
        command.setBizRefundNo("MANUAL-REFUND-001");
        command.setRefundAmount(3000L);
        command.setReason("后台受控退款");
        command.setRemark("退款备注");
        return command;
    }

    private PaymentApplication application() {
        PaymentApplication application = new PaymentApplication();
        application.setId(310001L);
        application.setTenantId(1L);
        application.setAppId("app_openapi");
        application.setStatus(1);
        return application;
    }

    private PaymentOrderVO paymentOrder() {
        PaymentOrderVO vo = new PaymentOrderVO();
        vo.setId(370001L);
        vo.setBusinessOrderId(360001L);
        vo.setPayOrderNo("PO202606070001");
        vo.setBizOrderNo("BIZ-001");
        vo.setAppId("app_openapi");
        vo.setAmount(8800L);
        vo.setStatus("SUCCESS");
        vo.setSuccessFlag(1);
        return vo;
    }

    private WorkflowProcessInstanceVO workflowProcess() {
        WorkflowProcessInstanceVO vo = new WorkflowProcessInstanceVO();
        vo.setProcessInstanceId("PROC-1");
        vo.setApplyId(390001L);
        vo.setCurrentTaskName("财务审批");
        return vo;
    }

    private PaymentRefundApprovalEntity approvalEntity(String status) {
        PaymentRefundApprovalEntity entity = new PaymentRefundApprovalEntity();
        entity.setId(390001L);
        entity.setApprovalNo("RFA202606070001");
        entity.setPaymentOrderId(370001L);
        entity.setBusinessOrderId(360001L);
        entity.setBizOrderNo("BIZ-001");
        entity.setBizRefundNo("MANUAL-REFUND-001");
        entity.setAppId("app_openapi");
        entity.setRefundAmount(3000L);
        entity.setReason("后台受控退款");
        entity.setStatus(status);
        entity.setTenantId(1L);
        entity.setApplyTime(LocalDateTime.of(2026, 6, 7, 10, 0));
        return entity;
    }

    private PaymentRefundApprovalVO approvalDetail(String status) {
        PaymentRefundApprovalVO vo = new PaymentRefundApprovalVO();
        vo.setId(390001L);
        vo.setApprovalNo("RFA202606070001");
        vo.setStatus(status);
        return vo;
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
