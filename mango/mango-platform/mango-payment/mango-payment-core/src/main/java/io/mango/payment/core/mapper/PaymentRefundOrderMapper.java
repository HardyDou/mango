package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentRefundOrderProjection;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentRefundOrderMapper extends BaseMapper<PaymentRefundOrderEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countRefundOrders(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    long countRefundOrdersByStatus(
            @Param("tenantId") String tenantId,
            @Param("status") String status);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentRefundOrderProjection> selectRefundOrderPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderProjection selectRefundOrderDetail(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderProjection selectOpenRefundOrder(
            @Param("tenantId") String tenantId,
            @Param("appId") String appId,
            @Param("bizRefundNo") String bizRefundNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderProjection selectByTenantAndRefundOrderNo(
            @Param("tenantId") String tenantId,
            @Param("refundOrderNo") String refundOrderNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderProjection selectByTenantAndChannelRefundNo(
            @Param("tenantId") String tenantId,
            @Param("channelRefundNo") String channelRefundNo);

    @InterceptorIgnore(tenantLine = "true")
    int updateRefundingQueryResult(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("status") String status,
            @Param("refundTime") LocalDateTime refundTime);

    @InterceptorIgnore(tenantLine = "true")
    int updateRefundApplyResult(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("channelRefundNo") String channelRefundNo,
            @Param("status") String status);

    @InterceptorIgnore(tenantLine = "true")
    Long sumOccupyingRefundAmount(
            @Param("tenantId") String tenantId,
            @Param("paymentOrderId") Long paymentOrderId);

    @InterceptorIgnore(tenantLine = "true")
    String selectLatestFlowNo(
            @Param("tenantId") String tenantId,
            @Param("refundOrderId") Long refundOrderId);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderEntity selectEntityByTenantAndChannelRefundNo(
            @Param("tenantId") String tenantId,
            @Param("channelRefundNo") String channelRefundNo);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentRefundOrderEntity> selectSuccessfulChannelRefundsMissingInBill(
            @Param("tenantId") String tenantId,
            @Param("channelCode") String channelCode,
            @Param("billDate") LocalDate billDate,
            @Param("nextBillDate") LocalDate nextBillDate,
            @Param("channelRefundNos") List<String> channelRefundNos);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentRefundOrderEntity> selectSuccessfulChannelRefundsForBill(
            @Param("tenantId") String tenantId,
            @Param("channelCode") String channelCode,
            @Param("contractId") Long contractId,
            @Param("billDate") LocalDate billDate,
            @Param("nextBillDate") LocalDate nextBillDate);
}
