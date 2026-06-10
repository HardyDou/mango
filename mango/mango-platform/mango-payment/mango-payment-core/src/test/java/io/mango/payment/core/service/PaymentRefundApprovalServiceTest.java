package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreatePaymentOpenRefundCommand;
import io.mango.payment.api.command.CreatePaymentRefundApprovalCommand;
import io.mango.payment.api.command.ReviewPaymentRefundApprovalCommand;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundApprovalVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentRefundApprovalEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundApprovalMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
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
        when(numberService.next(PaymentNumberService.PAY_BIZ_REFUND_NO)).thenReturn("BR2026060600000001");
        when(numberService.next(PaymentNumberService.PAY_REFUND_APPROVAL_NO)).thenReturn("RA2026060600000001");
        service = new PaymentRefundApprovalService(
                refundApprovalMapper,
                applicationMapper,
                paymentOrderMapper,
                refundOrderMapper,
                refundApplyService,
                orderStateService,
                auditService,
                numberService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "applicant", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("createRefundApproval should validate refundable amount and record audit")
    void createRefundApproval_validCommand_recordsAudit() {
        PaymentOrderVO paymentOrder = paymentOrder();
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(1000L);
        PaymentRefundApprovalVO detail = approvalDetail("PENDING");
        doAnswer(invocation -> {
            PaymentRefundApprovalEntity entity = invocation.getArgument(0);
            entity.setId(390001L);
            return 1;
        }).when(refundApprovalMapper).insert(any(PaymentRefundApprovalEntity.class));
        when(refundApprovalMapper.selectRefundApprovalDetail(1L, 390001L)).thenReturn(detail);
        ArgumentCaptor<PaymentRefundApprovalEntity> approvalCaptor = ArgumentCaptor.forClass(PaymentRefundApprovalEntity.class);

        PaymentRefundApprovalVO result = service.createRefundApproval(createCommand());

        assertThat(result.getStatusName()).isEqualTo("待审核");
        verify(refundApprovalMapper).insert(approvalCaptor.capture());
        PaymentRefundApprovalEntity entity = approvalCaptor.getValue();
        assertThat(entity.getPaymentOrderId()).isEqualTo(370001L);
        assertThat(entity.getBizRefundNo()).isEqualTo("MANUAL-REFUND-001");
        assertThat(entity.getRefundAmount()).isEqualTo(3000L);
        assertThat(entity.getRemark()).isEqualTo("退款备注");
        assertThat(entity.getStatus()).isEqualTo("PENDING");
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CREATE_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                entity.getApprovalNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("createRefundApproval should generate biz refund number on server when omitted")
    void createRefundApproval_withoutBizRefundNo_generatesServerNumber() {
        PaymentOrderVO paymentOrder = paymentOrder();
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrder);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(1000L);
        doAnswer(invocation -> {
            PaymentRefundApprovalEntity entity = invocation.getArgument(0);
            entity.setId(390001L);
            return 1;
        }).when(refundApprovalMapper).insert(any(PaymentRefundApprovalEntity.class));
        when(refundApprovalMapper.selectRefundApprovalDetail(1L, 390001L)).thenReturn(approvalDetail("PENDING"));
        CreatePaymentRefundApprovalCommand command = createCommand();
        command.setBizRefundNo(null);
        ArgumentCaptor<PaymentRefundApprovalEntity> approvalCaptor = ArgumentCaptor.forClass(PaymentRefundApprovalEntity.class);

        service.createRefundApproval(command);

        verify(refundApprovalMapper).insert(approvalCaptor.capture());
        assertThat(approvalCaptor.getValue().getBizRefundNo()).startsWith("BR");
    }

    @Test
    @DisplayName("reviewRefundApproval should reject self review")
    void reviewRefundApproval_selfReview_rejects() {
        PaymentRefundApprovalEntity entity = approvalEntity("PENDING");
        entity.setApplicantId(1001L);
        when(refundApprovalMapper.selectEntityForUpdate(1L, 390001L)).thenReturn(entity);

        assertThatThrownBy(() -> service.reviewRefundApproval(reviewCommand("APPROVE")))
                .isInstanceOf(BizException.class)
                .hasMessage("退款审批申请人不能审核自己的申请");
    }

    @Test
    @DisplayName("reviewRefundApproval approve should create refund through shared refund service")
    void reviewRefundApproval_approve_appliesRefundAndRecordsAudit() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                2002L, "1", "reviewer", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        PaymentRefundApprovalEntity entity = approvalEntity("PENDING");
        entity.setApplicantId(1001L);
        when(refundApprovalMapper.selectEntityForUpdate(1L, 390001L)).thenReturn(entity);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        PaymentOpenRefundOrderVO refundOrder = new PaymentOpenRefundOrderVO();
        refundOrder.setId(380001L);
        refundOrder.setRefundOrderNo("RO202606070001");
        when(refundApplyService.applyRefund(any(), any(), eq(PaymentOrderStatusFlowService.SOURCE_MANUAL_REFUND_APPROVAL), eq("RFA202606070001"), eq(true)))
                .thenReturn(refundOrder);
        when(refundApprovalMapper.selectRefundApprovalDetail(1L, 390001L)).thenReturn(approvalDetail("APPROVED"));
        ArgumentCaptor<CreatePaymentOpenRefundCommand> refundCommandCaptor = ArgumentCaptor.forClass(CreatePaymentOpenRefundCommand.class);

        PaymentRefundApprovalVO result = service.reviewRefundApproval(reviewCommand("APPROVE"));

        assertThat(result.getStatusName()).isEqualTo("已通过");
        verify(refundApplyService).applyRefund(
                any(),
                refundCommandCaptor.capture(),
                eq(PaymentOrderStatusFlowService.SOURCE_MANUAL_REFUND_APPROVAL),
                eq("RFA202606070001"),
                eq(true));
        assertThat(refundCommandCaptor.getValue().getBizRefundNo()).isEqualTo("MANUAL-REFUND-001");
        assertThat(entity.getRefundOrderId()).isEqualTo(380001L);
        assertThat(entity.getStatus()).isEqualTo("APPROVED");
        verify(refundApprovalMapper).updateById(entity);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_APPROVE_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                "RFA202606070001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("reviewRefundApproval reject should close approval and record audit")
    void reviewRefundApproval_reject_recordsAudit() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                2002L, "1", "reviewer", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        PaymentRefundApprovalEntity entity = approvalEntity("PENDING");
        entity.setApplicantId(1001L);
        when(refundApprovalMapper.selectEntityForUpdate(1L, 390001L)).thenReturn(entity);
        when(refundApprovalMapper.selectRefundApprovalDetail(1L, 390001L)).thenReturn(approvalDetail("REJECTED"));

        PaymentRefundApprovalVO result = service.reviewRefundApproval(reviewCommand("REJECT"));

        assertThat(result.getStatusName()).isEqualTo("已拒绝");
        assertThat(entity.getStatus()).isEqualTo("REJECTED");
        assertThat(entity.getReviewerId()).isEqualTo(2002L);
        assertThat(entity.getReviewerName()).isEqualTo("reviewer");
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

    private ReviewPaymentRefundApprovalCommand reviewCommand(String action) {
        ReviewPaymentRefundApprovalCommand command = new ReviewPaymentRefundApprovalCommand();
        command.setId(390001L);
        command.setAction(action);
        command.setReviewReason("审批通过");
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
}
