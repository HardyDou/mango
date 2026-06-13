package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.ConfirmPaymentSettlementSummaryCommand;
import io.mango.payment.api.command.GeneratePaymentSettlementSummaryCommand;
import io.mango.payment.api.command.VoidPaymentSettlementSummaryCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentSettlementSummaryStatusVO;
import io.mango.payment.api.vo.PaymentSettlementSummaryVO;
import io.mango.payment.core.entity.PaymentSettlementSummaryEntity;
import io.mango.payment.core.mapper.PaymentSettlementSummaryMapper;
import io.mango.payment.core.model.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentSettlementSummaryService {

    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_VOIDED = "VOIDED";

    private final PaymentSettlementSummaryMapper settlementSummaryMapper;
    private final PaymentOperationAuditService auditService;

    public PageResult<PaymentSettlementSummaryVO> pageSettlementSummaries(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = normalizeCode(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = settlementSummaryMapper.countSettlementSummaries(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentSettlementSummaryVO> rows = settlementSummaryMapper.selectSettlementSummaryPage(
                tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillSettlementSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentSettlementSummaryVO detailSettlementSummary(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "结算汇总 ID 不能为空");
        PaymentSettlementSummaryVO vo = settlementSummaryMapper.selectSettlementSummaryDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_NOT_FOUND);
        fillSettlementSummary(vo);
        return vo;
    }

    public List<PaymentSettlementSummaryStatusVO> listSettlementSummaryStatuses() {
        return List.of(
                settlementStatus(STATUS_GENERATED),
                settlementStatus(STATUS_CONFIRMED),
                settlementStatus(STATUS_VOIDED));
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentSettlementSummaryVO generateSettlementSummary(GeneratePaymentSettlementSummaryCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID);
        NormalizedScope scope = normalizeScope(command);
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentSettlementSummaryEntity existing = settlementSummaryMapper.selectByScope(
                tenantId, scope.settlementDate(), scope.appCode(), scope.enterpriseSubjectId(), scope.channelCode());
        boolean rebuild = Boolean.TRUE.equals(command.getRebuild());
        Require.isTrue(existing == null,
                PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_STATUS_INVALID.getCode(),
                rebuild ? "已存在未作废结算汇总，不能重新生成" : "已存在未作废结算汇总，不能重复生成");
        Require.isTrue(hasCompletedReconciliation(tenantId, scope), PaymentCode.PAYMENT_SETTLEMENT_RECONCILIATION_REQUIRED);

        SettlementSnapshot snapshot = calculateSnapshot(tenantId, scope);
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        PaymentSettlementSummaryEntity entity = new PaymentSettlementSummaryEntity();
        entity.setId(IdWorker.getId());
        entity.setTenantId(tenantId);
        entity.setCreatedBy(operatorId);
        entity.setCreatedAt(now);
        entity.setSettlementDate(scope.settlementDate());
        entity.setAppCode(scope.appCode());
        entity.setEnterpriseSubjectId(scope.enterpriseSubjectId());
        entity.setChannelCode(scope.channelCode());
        entity.setTradeAmount(snapshot.tradeAmount());
        entity.setRefundAmount(snapshot.refundAmount());
        entity.setFeeAmount(snapshot.feeAmount());
        entity.setNetAmount(snapshot.netAmount());
        entity.setTradeCount(snapshot.tradeCount());
        entity.setRefundCount(snapshot.refundCount());
        entity.setUnresolvedDifferenceCount(snapshot.unresolvedDifferenceCount());
        entity.setUnresolvedDifferenceAmount(snapshot.unresolvedDifferenceAmount());
        entity.setStatus(STATUS_GENERATED);
        entity.setGeneratedBy(operatorId);
        entity.setGeneratedByName(PaymentContextSupport.currentPrincipalName());
        entity.setGeneratedAt(now);
        entity.setConfirmedBy(null);
        entity.setConfirmedByName(null);
        entity.setConfirmedAt(null);
        entity.setVoidedBy(null);
        entity.setVoidedByName(null);
        entity.setVoidedAt(null);
        entity.setVoidReason(null);
        entity.setUpdatedBy(operatorId);
        entity.setUpdatedAt(now);
        settlementSummaryMapper.insert(entity);
        auditService.record(
                rebuild ? PaymentOperationAuditService.ACTION_REBUILD_SETTLEMENT_SUMMARY : PaymentOperationAuditService.ACTION_GENERATE_SETTLEMENT_SUMMARY,
                PaymentOperationAuditService.RESOURCE_PAYMENT_SETTLEMENT_SUMMARY,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailSettlementSummary(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentSettlementSummaryVO confirmSettlementSummary(ConfirmPaymentSettlementSummaryCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "结算汇总 ID 不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentSettlementSummaryEntity entity = settlementSummaryMapper.selectById(command.getId());
        Require.notNull(entity, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_NOT_FOUND);
        Require.isTrue(tenantId.equals(entity.getTenantId()) && Integer.valueOf(0).equals(entity.getDelFlag()),
                PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_NOT_FOUND);
        Require.isTrue(STATUS_GENERATED.equals(entity.getStatus()), PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_STATUS_INVALID);

        NormalizedScope scope = new NormalizedScope(
                entity.getSettlementDate(), entity.getAppCode(), entity.getEnterpriseSubjectId(), entity.getChannelCode());
        Require.isTrue(hasCompletedReconciliation(tenantId, scope), PaymentCode.PAYMENT_SETTLEMENT_RECONCILIATION_REQUIRED);
        SettlementSnapshot snapshot = calculateSnapshot(tenantId, scope);
        Require.isTrue(snapshot.unresolvedDifferenceCount() == 0, PaymentCode.PAYMENT_SETTLEMENT_UNRESOLVED_DIFFERENCE);

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        int updated = settlementSummaryMapper.confirmGeneratedSummary(
                tenantId,
                entity.getId(),
                snapshot.tradeAmount(),
                snapshot.refundAmount(),
                snapshot.feeAmount(),
                snapshot.netAmount(),
                snapshot.tradeCount(),
                snapshot.refundCount(),
                snapshot.unresolvedDifferenceCount(),
                snapshot.unresolvedDifferenceAmount(),
                operatorId,
                PaymentContextSupport.currentPrincipalName(),
                now);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_STATUS_INVALID);
        auditService.record(
                PaymentOperationAuditService.ACTION_CONFIRM_SETTLEMENT_SUMMARY,
                PaymentOperationAuditService.RESOURCE_PAYMENT_SETTLEMENT_SUMMARY,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailSettlementSummary(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentSettlementSummaryVO voidSettlementSummary(VoidPaymentSettlementSummaryCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "结算汇总 ID 不能为空");
        String reason = PaymentContextSupport.trimToNull(command.getVoidReason());
        Require.notBlank(reason, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "作废原因不能为空");
        Require.isTrue(reason.length() <= 512, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "作废原因不能超过 512 个字符");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentSettlementSummaryEntity entity = settlementSummaryMapper.selectById(command.getId());
        Require.notNull(entity, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_NOT_FOUND);
        Require.isTrue(tenantId.equals(entity.getTenantId()) && Integer.valueOf(0).equals(entity.getDelFlag()),
                PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_NOT_FOUND);
        Require.isTrue(STATUS_CONFIRMED.equals(entity.getStatus()), PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_STATUS_INVALID);

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        int updated = settlementSummaryMapper.voidConfirmedSummary(
                tenantId,
                entity.getId(),
                operatorId,
                PaymentContextSupport.currentPrincipalName(),
                now,
                reason);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_STATUS_INVALID);
        auditService.record(
                PaymentOperationAuditService.ACTION_VOID_SETTLEMENT_SUMMARY,
                PaymentOperationAuditService.RESOURCE_PAYMENT_SETTLEMENT_SUMMARY,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailSettlementSummary(entity.getId());
    }

    private NormalizedScope normalizeScope(GeneratePaymentSettlementSummaryCommand command) {
        Require.notNull(command.getSettlementDate(), PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "结算日期不能为空");
        String appCode = normalizeCode(command.getAppCode());
        String channelCode = normalizeCode(command.getChannelCode());
        Require.notBlank(appCode, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "应用编码不能为空");
        Require.notNull(command.getEnterpriseSubjectId(), PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "企业主体 ID 不能为空");
        Require.notBlank(channelCode, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "通道编码不能为空");
        Require.isTrue(appCode.length() <= 64, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "应用编码不能超过 64 个字符");
        Require.isTrue(channelCode.length() <= 32, PaymentCode.PAYMENT_SETTLEMENT_SUMMARY_INVALID.getCode(), "通道编码不能超过 32 个字符");
        return new NormalizedScope(command.getSettlementDate(), appCode, command.getEnterpriseSubjectId(), channelCode);
    }

    private SettlementSnapshot calculateSnapshot(Long tenantId, NormalizedScope scope) {
        PaymentSettlementSummaryMapper.SettlementCalculation calculation = settlementSummaryMapper.selectSettlementCalculation(
                tenantId, scope.settlementDate(), scope.appCode(), scope.enterpriseSubjectId(), scope.channelCode());
        PaymentSettlementSummaryMapper.DifferenceCalculation differences = settlementSummaryMapper.selectUnresolvedDifferenceCalculation(
                tenantId, scope.settlementDate(), scope.appCode(), scope.enterpriseSubjectId(), scope.channelCode());
        long tradeAmount = amount(calculation == null ? null : calculation.getTradeAmount());
        long refundAmount = amount(calculation == null ? null : calculation.getRefundAmount());
        long feeAmount = amount(calculation == null ? null : calculation.getFeeAmount());
        int tradeCount = count(calculation == null ? null : calculation.getTradeCount());
        int refundCount = count(calculation == null ? null : calculation.getRefundCount());
        int unresolvedCount = count(differences == null ? null : differences.getUnresolvedDifferenceCount());
        long unresolvedAmount = amount(differences == null ? null : differences.getUnresolvedDifferenceAmount());
        long netAmount = Money.cents(tradeAmount)
                .subtract(Money.cents(refundAmount))
                .subtract(Money.cents(feeAmount))
                .toNonNegativeCents();
        return new SettlementSnapshot(
                tradeAmount,
                refundAmount,
                feeAmount,
                netAmount,
                tradeCount,
                refundCount,
                unresolvedCount,
                unresolvedAmount);
    }

    private boolean hasCompletedReconciliation(Long tenantId, NormalizedScope scope) {
        return settlementSummaryMapper.countCompletedReconciliation(
                tenantId, scope.settlementDate(), scope.channelCode()) > 0;
    }

    private void fillSettlementSummary(PaymentSettlementSummaryVO vo) {
        vo.setStatusName(settlementStatusName(vo.getStatus()));
    }

    private PaymentSettlementSummaryStatusVO settlementStatus(String status) {
        PaymentSettlementSummaryStatusVO vo = new PaymentSettlementSummaryStatusVO();
        vo.setStatusCode(status);
        vo.setStatusName(settlementStatusName(status));
        return vo;
    }

    private String settlementStatusName(String status) {
        if (STATUS_GENERATED.equals(status)) {
            return "已生成";
        }
        if (STATUS_CONFIRMED.equals(status)) {
            return "已确认";
        }
        if (STATUS_VOIDED.equals(status)) {
            return "已作废";
        }
        return status;
    }

    private String normalizeCode(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private long amount(Long value) {
        return Money.cents(value == null ? 0L : value).toNonNegativeCents();
    }

    private int count(Integer value) {
        return value == null ? 0 : value;
    }

    private record NormalizedScope(
            java.time.LocalDate settlementDate,
            String appCode,
            Long enterpriseSubjectId,
            String channelCode
    ) {
    }

    private record SettlementSnapshot(
            long tradeAmount,
            long refundAmount,
            long feeAmount,
            long netAmount,
            int tradeCount,
            int refundCount,
            int unresolvedDifferenceCount,
            long unresolvedDifferenceAmount
    ) {
    }
}
