package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentSettlementSummaryVO;
import io.mango.payment.core.entity.PaymentSettlementSummaryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentSettlementSummaryMapper extends BaseMapper<PaymentSettlementSummaryEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countSettlementSummaries(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentSettlementSummaryVO> selectSettlementSummaryPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentSettlementSummaryVO selectSettlementSummaryDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentSettlementSummaryEntity selectByScope(
            @Param("tenantId") Long tenantId,
            @Param("settlementDate") LocalDate settlementDate,
            @Param("appCode") String appCode,
            @Param("enterpriseSubjectId") Long enterpriseSubjectId,
            @Param("channelCode") String channelCode);

    @InterceptorIgnore(tenantLine = "true")
    SettlementCalculation selectSettlementCalculation(
            @Param("tenantId") Long tenantId,
            @Param("settlementDate") LocalDate settlementDate,
            @Param("appCode") String appCode,
            @Param("enterpriseSubjectId") Long enterpriseSubjectId,
            @Param("channelCode") String channelCode);

    @InterceptorIgnore(tenantLine = "true")
    long countCompletedReconciliation(
            @Param("tenantId") Long tenantId,
            @Param("settlementDate") LocalDate settlementDate,
            @Param("channelCode") String channelCode);

    @InterceptorIgnore(tenantLine = "true")
    DifferenceCalculation selectUnresolvedDifferenceCalculation(
            @Param("tenantId") Long tenantId,
            @Param("settlementDate") LocalDate settlementDate,
            @Param("appCode") String appCode,
            @Param("enterpriseSubjectId") Long enterpriseSubjectId,
            @Param("channelCode") String channelCode);

    @InterceptorIgnore(tenantLine = "true")
    int confirmGeneratedSummary(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("tradeAmount") Long tradeAmount,
            @Param("refundAmount") Long refundAmount,
            @Param("feeAmount") Long feeAmount,
            @Param("netAmount") Long netAmount,
            @Param("tradeCount") Integer tradeCount,
            @Param("refundCount") Integer refundCount,
            @Param("unresolvedDifferenceCount") Integer unresolvedDifferenceCount,
            @Param("unresolvedDifferenceAmount") Long unresolvedDifferenceAmount,
            @Param("confirmedBy") Long confirmedBy,
            @Param("confirmedByName") String confirmedByName,
            @Param("confirmedAt") LocalDateTime confirmedAt);

    @InterceptorIgnore(tenantLine = "true")
    int voidConfirmedSummary(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("voidedBy") Long voidedBy,
            @Param("voidedByName") String voidedByName,
            @Param("voidedAt") LocalDateTime voidedAt,
            @Param("voidReason") String voidReason);

    class SettlementCalculation {

        private Long tradeAmount;
        private Long refundAmount;
        private Long feeAmount;
        private Integer tradeCount;
        private Integer refundCount;

        public Long getTradeAmount() {
            return tradeAmount;
        }

        public void setTradeAmount(Long tradeAmount) {
            this.tradeAmount = tradeAmount;
        }

        public Long getRefundAmount() {
            return refundAmount;
        }

        public void setRefundAmount(Long refundAmount) {
            this.refundAmount = refundAmount;
        }

        public Long getFeeAmount() {
            return feeAmount;
        }

        public void setFeeAmount(Long feeAmount) {
            this.feeAmount = feeAmount;
        }

        public Integer getTradeCount() {
            return tradeCount;
        }

        public void setTradeCount(Integer tradeCount) {
            this.tradeCount = tradeCount;
        }

        public Integer getRefundCount() {
            return refundCount;
        }

        public void setRefundCount(Integer refundCount) {
            this.refundCount = refundCount;
        }
    }

    class DifferenceCalculation {

        private Integer unresolvedDifferenceCount;
        private Long unresolvedDifferenceAmount;

        public Integer getUnresolvedDifferenceCount() {
            return unresolvedDifferenceCount;
        }

        public void setUnresolvedDifferenceCount(Integer unresolvedDifferenceCount) {
            this.unresolvedDifferenceCount = unresolvedDifferenceCount;
        }

        public Long getUnresolvedDifferenceAmount() {
            return unresolvedDifferenceAmount;
        }

        public void setUnresolvedDifferenceAmount(Long unresolvedDifferenceAmount) {
            this.unresolvedDifferenceAmount = unresolvedDifferenceAmount;
        }
    }
}
