package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.ConfirmPaymentSettlementSummaryCommand;
import io.mango.payment.api.command.GeneratePaymentSettlementSummaryCommand;
import io.mango.payment.api.command.VoidPaymentSettlementSummaryCommand;
import io.mango.payment.api.vo.PaymentSettlementSummaryVO;
import io.mango.payment.core.entity.PaymentSettlementSummaryEntity;
import io.mango.payment.core.mapper.PaymentSettlementSummaryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentSettlementSummaryServiceTest {

    private PaymentSettlementSummaryMapper settlementSummaryMapper;
    private PaymentOperationAuditService auditService;
    private PaymentSettlementSummaryService service;

    @BeforeEach
    void setUp() {
        settlementSummaryMapper = mock(PaymentSettlementSummaryMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        service = new PaymentSettlementSummaryService(settlementSummaryMapper, auditService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("generateSettlementSummary should calculate cents and record audit")
    void generateSettlementSummary_calculatesAndAudits() {
        GeneratePaymentSettlementSummaryCommand command = new GeneratePaymentSettlementSummaryCommand();
        command.setSettlementDate(LocalDate.of(2026, 6, 1));
        command.setAppCode("app_order");
        command.setEnterpriseSubjectId(320001L);
        command.setChannelCode("MANGO_PAY");

        PaymentSettlementSummaryMapper.SettlementCalculation calculation = new PaymentSettlementSummaryMapper.SettlementCalculation();
        calculation.setTradeAmount(10000L);
        calculation.setRefundAmount(2100L);
        calculation.setFeeAmount(130L);
        calculation.setTradeCount(2);
        calculation.setRefundCount(1);
        when(settlementSummaryMapper.countCompletedReconciliation(1L, LocalDate.of(2026, 6, 1), "MANGO_PAY"))
                .thenReturn(1L);
        PaymentSettlementSummaryMapper.DifferenceCalculation difference = new PaymentSettlementSummaryMapper.DifferenceCalculation();
        difference.setUnresolvedDifferenceCount(0);
        difference.setUnresolvedDifferenceAmount(0L);
        when(settlementSummaryMapper.selectSettlementCalculation(1L, LocalDate.of(2026, 6, 1), "APP_ORDER", 320001L, "MANGO_PAY"))
                .thenReturn(calculation);
        when(settlementSummaryMapper.selectUnresolvedDifferenceCalculation(1L, LocalDate.of(2026, 6, 1), "APP_ORDER", 320001L, "MANGO_PAY"))
                .thenReturn(difference);
        PaymentSettlementSummaryVO detail = new PaymentSettlementSummaryVO();
        detail.setId(500001L);
        detail.setStatus("GENERATED");
        when(settlementSummaryMapper.selectSettlementSummaryDetail(any(), any())).thenReturn(detail);

        PaymentSettlementSummaryVO result = service.generateSettlementSummary(command);

        assertThat(result.getStatusName()).isEqualTo("已生成");
        ArgumentCaptor<PaymentSettlementSummaryEntity> entityCaptor = ArgumentCaptor.forClass(PaymentSettlementSummaryEntity.class);
        verify(settlementSummaryMapper).insert(entityCaptor.capture());
        PaymentSettlementSummaryEntity inserted = entityCaptor.getValue();
        assertThat(inserted.getTradeAmount()).isEqualTo(10000L);
        assertThat(inserted.getRefundAmount()).isEqualTo(2100L);
        assertThat(inserted.getFeeAmount()).isEqualTo(130L);
        assertThat(inserted.getNetAmount()).isEqualTo(7770L);
        verify(auditService).record(
                eq(PaymentOperationAuditService.ACTION_GENERATE_SETTLEMENT_SUMMARY),
                eq(PaymentOperationAuditService.RESOURCE_PAYMENT_SETTLEMENT_SUMMARY),
                eq(String.valueOf(inserted.getId())),
                eq(PaymentOperationAuditService.RESULT_SUCCESS));
    }

    @Test
    @DisplayName("generateSettlementSummary should require completed reconciliation")
    void generateSettlementSummary_withoutCompletedReconciliation_rejects() {
        GeneratePaymentSettlementSummaryCommand command = new GeneratePaymentSettlementSummaryCommand();
        command.setSettlementDate(LocalDate.of(2026, 6, 1));
        command.setAppCode("app_order");
        command.setEnterpriseSubjectId(320001L);
        command.setChannelCode("MANGO_PAY");
        when(settlementSummaryMapper.countCompletedReconciliation(1L, LocalDate.of(2026, 6, 1), "MANGO_PAY"))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.generateSettlementSummary(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_SETTLEMENT_RECONCILIATION_REQUIRED.getMessage());
        verify(settlementSummaryMapper, never()).insert(any(PaymentSettlementSummaryEntity.class));
    }

    @Test
    @DisplayName("confirmSettlementSummary should reject unresolved differences")
    void confirmSettlementSummary_withUnresolvedDifference_rejects() {
        PaymentSettlementSummaryEntity entity = generatedEntity();
        when(settlementSummaryMapper.selectById(500001L)).thenReturn(entity);
        when(settlementSummaryMapper.countCompletedReconciliation(1L, entity.getSettlementDate(), entity.getChannelCode()))
                .thenReturn(1L);
        PaymentSettlementSummaryMapper.SettlementCalculation calculation = new PaymentSettlementSummaryMapper.SettlementCalculation();
        calculation.setTradeAmount(10000L);
        calculation.setRefundAmount(1000L);
        calculation.setFeeAmount(100L);
        calculation.setTradeCount(1);
        calculation.setRefundCount(1);
        when(settlementSummaryMapper.selectSettlementCalculation(1L, entity.getSettlementDate(), entity.getAppCode(), entity.getEnterpriseSubjectId(), entity.getChannelCode()))
                .thenReturn(calculation);
        PaymentSettlementSummaryMapper.DifferenceCalculation difference = new PaymentSettlementSummaryMapper.DifferenceCalculation();
        difference.setUnresolvedDifferenceCount(1);
        difference.setUnresolvedDifferenceAmount(100L);
        when(settlementSummaryMapper.selectUnresolvedDifferenceCalculation(1L, entity.getSettlementDate(), entity.getAppCode(), entity.getEnterpriseSubjectId(), entity.getChannelCode()))
                .thenReturn(difference);

        ConfirmPaymentSettlementSummaryCommand command = new ConfirmPaymentSettlementSummaryCommand();
        command.setId(500001L);

        assertThatThrownBy(() -> service.confirmSettlementSummary(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_SETTLEMENT_UNRESOLVED_DIFFERENCE.getMessage());
    }

    @Test
    @DisplayName("generateSettlementSummary should reject negative net amount through money boundary")
    void generateSettlementSummary_negativeNetAmount_rejects() {
        GeneratePaymentSettlementSummaryCommand command = new GeneratePaymentSettlementSummaryCommand();
        command.setSettlementDate(LocalDate.of(2026, 6, 1));
        command.setAppCode("app_order");
        command.setEnterpriseSubjectId(320001L);
        command.setChannelCode("MANGO_PAY");

        PaymentSettlementSummaryMapper.SettlementCalculation calculation = new PaymentSettlementSummaryMapper.SettlementCalculation();
        calculation.setTradeAmount(1000L);
        calculation.setRefundAmount(1200L);
        calculation.setFeeAmount(0L);
        calculation.setTradeCount(1);
        calculation.setRefundCount(1);
        when(settlementSummaryMapper.countCompletedReconciliation(1L, LocalDate.of(2026, 6, 1), "MANGO_PAY"))
                .thenReturn(1L);
        when(settlementSummaryMapper.selectSettlementCalculation(1L, LocalDate.of(2026, 6, 1), "APP_ORDER", 320001L, "MANGO_PAY"))
                .thenReturn(calculation);

        assertThatThrownBy(() -> service.generateSettlementSummary(command))
                .isInstanceOf(BizException.class)
                .hasMessage("金额不能小于 0 分");
    }

    @Test
    @DisplayName("voidSettlementSummary should only allow confirmed summaries and audit")
    void voidSettlementSummary_confirmedRow_voidsAndAudits() {
        PaymentSettlementSummaryEntity entity = generatedEntity();
        entity.setStatus("CONFIRMED");
        when(settlementSummaryMapper.selectById(500001L)).thenReturn(entity);
        when(settlementSummaryMapper.voidConfirmedSummary(eq(1L), eq(500001L), eq(1001L), eq("admin"), any(), eq("账单修正后重新生成")))
                .thenReturn(1);
        PaymentSettlementSummaryVO detail = new PaymentSettlementSummaryVO();
        detail.setId(500001L);
        detail.setStatus("VOIDED");
        when(settlementSummaryMapper.selectSettlementSummaryDetail(1L, 500001L)).thenReturn(detail);

        VoidPaymentSettlementSummaryCommand command = new VoidPaymentSettlementSummaryCommand();
        command.setId(500001L);
        command.setVoidReason("账单修正后重新生成");

        PaymentSettlementSummaryVO result = service.voidSettlementSummary(command);

        assertThat(result.getStatusName()).isEqualTo("已作废");
        verify(settlementSummaryMapper).voidConfirmedSummary(eq(1L), eq(500001L), eq(1001L), eq("admin"), any(), eq("账单修正后重新生成"));
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_VOID_SETTLEMENT_SUMMARY,
                PaymentOperationAuditService.RESOURCE_PAYMENT_SETTLEMENT_SUMMARY,
                "500001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("confirmSettlementSummary should reject cross tenant row before update")
    void confirmSettlementSummary_crossTenant_rejectsBeforeUpdate() {
        PaymentSettlementSummaryEntity entity = generatedEntity();
        entity.setTenantId(2L);
        when(settlementSummaryMapper.selectById(500001L)).thenReturn(entity);
        ConfirmPaymentSettlementSummaryCommand command = new ConfirmPaymentSettlementSummaryCommand();
        command.setId(500001L);

        assertThatThrownBy(() -> service.confirmSettlementSummary(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_NOT_FOUND.getMessage());
        verify(settlementSummaryMapper, never()).updateById(entity);
        verify(settlementSummaryMapper, never()).confirmGeneratedSummary(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("voidSettlementSummary should reject cross tenant row before update")
    void voidSettlementSummary_crossTenant_rejectsBeforeUpdate() {
        PaymentSettlementSummaryEntity entity = generatedEntity();
        entity.setTenantId(2L);
        entity.setStatus("CONFIRMED");
        when(settlementSummaryMapper.selectById(500001L)).thenReturn(entity);
        VoidPaymentSettlementSummaryCommand command = new VoidPaymentSettlementSummaryCommand();
        command.setId(500001L);
        command.setVoidReason("跨租户作废");

        assertThatThrownBy(() -> service.voidSettlementSummary(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_NOT_FOUND.getMessage());
        verify(settlementSummaryMapper, never()).updateById(entity);
        verify(settlementSummaryMapper, never()).voidConfirmedSummary(any(), any(), any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("confirmSettlementSummary should use controlled status update")
    void confirmSettlementSummary_generatedRow_confirmsWithControlledUpdate() {
        PaymentSettlementSummaryEntity entity = generatedEntity();
        when(settlementSummaryMapper.selectById(500001L)).thenReturn(entity);
        when(settlementSummaryMapper.countCompletedReconciliation(1L, entity.getSettlementDate(), entity.getChannelCode()))
                .thenReturn(1L);
        PaymentSettlementSummaryMapper.SettlementCalculation calculation = new PaymentSettlementSummaryMapper.SettlementCalculation();
        calculation.setTradeAmount(10000L);
        calculation.setRefundAmount(2100L);
        calculation.setFeeAmount(130L);
        calculation.setTradeCount(2);
        calculation.setRefundCount(1);
        when(settlementSummaryMapper.selectSettlementCalculation(1L, entity.getSettlementDate(), entity.getAppCode(), entity.getEnterpriseSubjectId(), entity.getChannelCode()))
                .thenReturn(calculation);
        PaymentSettlementSummaryMapper.DifferenceCalculation difference = new PaymentSettlementSummaryMapper.DifferenceCalculation();
        difference.setUnresolvedDifferenceCount(0);
        difference.setUnresolvedDifferenceAmount(0L);
        when(settlementSummaryMapper.selectUnresolvedDifferenceCalculation(1L, entity.getSettlementDate(), entity.getAppCode(), entity.getEnterpriseSubjectId(), entity.getChannelCode()))
                .thenReturn(difference);
        when(settlementSummaryMapper.confirmGeneratedSummary(
                eq(1L), eq(500001L), eq(10000L), eq(2100L), eq(130L), eq(7770L),
                eq(2), eq(1), eq(0), eq(0L), eq(1001L), eq("admin"), any(LocalDateTime.class)))
                .thenReturn(1);
        PaymentSettlementSummaryVO detail = new PaymentSettlementSummaryVO();
        detail.setId(500001L);
        detail.setStatus("CONFIRMED");
        when(settlementSummaryMapper.selectSettlementSummaryDetail(1L, 500001L)).thenReturn(detail);

        ConfirmPaymentSettlementSummaryCommand command = new ConfirmPaymentSettlementSummaryCommand();
        command.setId(500001L);

        PaymentSettlementSummaryVO result = service.confirmSettlementSummary(command);

        assertThat(result.getStatusName()).isEqualTo("已确认");
        verify(settlementSummaryMapper).confirmGeneratedSummary(
                eq(1L), eq(500001L), eq(10000L), eq(2100L), eq(130L), eq(7770L),
                eq(2), eq(1), eq(0), eq(0L), eq(1001L), eq("admin"), any(LocalDateTime.class));
        verify(settlementSummaryMapper, never()).updateById(entity);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CONFIRM_SETTLEMENT_SUMMARY,
                PaymentOperationAuditService.RESOURCE_PAYMENT_SETTLEMENT_SUMMARY,
                "500001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    private PaymentSettlementSummaryEntity generatedEntity() {
        PaymentSettlementSummaryEntity entity = new PaymentSettlementSummaryEntity();
        entity.setId(500001L);
        entity.setTenantId(1L);
        entity.setSettlementDate(LocalDate.of(2026, 6, 1));
        entity.setAppCode("APP_ORDER");
        entity.setEnterpriseSubjectId(320001L);
        entity.setChannelCode("MANGO_PAY");
        entity.setStatus("GENERATED");
        entity.setDelFlag(0);
        return entity;
    }
}
