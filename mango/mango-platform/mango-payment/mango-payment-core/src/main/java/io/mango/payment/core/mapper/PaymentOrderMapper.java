package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentOrderProjection;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.model.PaymentChannelFailureMetric;
import io.mango.payment.core.model.PaymentCashierPayResultRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrderEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countPaymentOrders(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("channelId") Long channelId);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOrderProjection> selectPaymentOrderPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("channelId") Long channelId,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOrderProjection selectPaymentOrderDetail(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOrderProjection selectPaymentOrderById(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOrderProjection selectOpenPaymentOrder(
            @Param("tenantId") String tenantId,
            @Param("appId") String appId,
            @Param("payOrderNo") String payOrderNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOrderProjection selectSuccessfulOpenPaymentOrder(
            @Param("tenantId") String tenantId,
            @Param("appId") String appId,
            @Param("bizOrderNo") String bizOrderNo);

    @InterceptorIgnore(tenantLine = "true")
    Long lockSuccessfulOpenPaymentOrder(
            @Param("tenantId") String tenantId,
            @Param("paymentOrderId") Long paymentOrderId);

    @InterceptorIgnore(tenantLine = "true")
    String selectLatestFlowNo(
            @Param("tenantId") String tenantId,
            @Param("paymentOrderId") Long paymentOrderId);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOrderEntity selectByTenantAndChannelTradeNo(
            @Param("tenantId") String tenantId,
            @Param("channelCode") String channelCode,
            @Param("channelTradeNo") String channelTradeNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOrderEntity selectByTenantAndPayOrderNo(
            @Param("tenantId") String tenantId,
            @Param("payOrderNo") String payOrderNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOrderEntity selectByPayOrderNo(
            @Param("payOrderNo") String payOrderNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOrderEntity selectEntityByTenantAndId(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOrderEntity> selectExpiredOpenPaymentOrders(
            @Param("tenantId") String tenantId,
            @Param("now") LocalDateTime now,
            @Param("limit") long limit);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOrderEntity> selectProcessingPaymentOrders(
            @Param("tenantId") String tenantId,
            @Param("limit") long limit);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOrderEntity> selectSuccessfulChannelOrdersMissingInBill(
            @Param("tenantId") String tenantId,
            @Param("channelCode") String channelCode,
            @Param("billDate") LocalDate billDate,
            @Param("nextBillDate") LocalDate nextBillDate,
            @Param("channelTradeNos") List<String> channelTradeNos);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOrderEntity> selectSuccessfulChannelOrdersForBill(
            @Param("tenantId") String tenantId,
            @Param("channelCode") String channelCode,
            @Param("contractId") Long contractId,
            @Param("billDate") LocalDate billDate,
            @Param("nextBillDate") LocalDate nextBillDate);

    @InterceptorIgnore(tenantLine = "true")
    int updatePayingQueryResult(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("status") String status,
            @Param("successFlag") Integer successFlag,
            @Param("payTime") LocalDateTime payTime);

    @InterceptorIgnore(tenantLine = "true")
    int updateCreatedApplyResult(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("status") String status,
            @Param("channelTradeNo") String channelTradeNo,
            @Param("paymentMaterialJson") String paymentMaterialJson);

    @InterceptorIgnore(tenantLine = "true")
    int markCreatedApplyFailed(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    int updatePayingCallbackResult(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("status") String status,
            @Param("successFlag") Integer successFlag,
            @Param("payTime") LocalDateTime payTime,
            @Param("channelTradeNo") String channelTradeNo);

    @InterceptorIgnore(tenantLine = "true")
    int updateOfflineCollectionSuccess(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("payTime") LocalDateTime payTime,
            @Param("channelTradeNo") String channelTradeNo);

    @InterceptorIgnore(tenantLine = "true")
    int markDuplicatePaymentSuccess(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("payTime") LocalDateTime payTime,
            @Param("channelTradeNo") String channelTradeNo);

    @InterceptorIgnore(tenantLine = "true")
    int markDuplicatePaymentRefunding(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    int markDuplicatePaymentRefunded(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    int closeOpenPaymentOrder(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    long countSuccessfulCashierOrders(
            @Param("tenantId") String tenantId,
            @Param("businessOrderId") Long businessOrderId);

    @InterceptorIgnore(tenantLine = "true")
    long countPaymentOrdersByStatus(
            @Param("tenantId") String tenantId,
            @Param("status") String status);

    @InterceptorIgnore(tenantLine = "true")
    long countProcessingPaymentBacklog(@Param("tenantId") String tenantId);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentChannelFailureMetric> selectChannelFailureMetrics(@Param("tenantId") String tenantId);

    String selectProcessingPayOrderNo(
            @Param("tenantId") String tenantId,
            @Param("businessOrderId") Long businessOrderId,
            @Param("methodId") Long methodId);

    PaymentCashierPayResultRow selectCashierPayResult(
            @Param("tenantId") String tenantId,
            @Param("payOrderNo") String payOrderNo);
}
