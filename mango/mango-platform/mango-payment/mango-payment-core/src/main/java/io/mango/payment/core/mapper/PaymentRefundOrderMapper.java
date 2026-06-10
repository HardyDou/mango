package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
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
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    long countRefundOrdersByStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") String status);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentRefundOrderVO> selectRefundOrderPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderVO selectRefundOrderDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderVO selectOpenRefundOrder(
            @Param("tenantId") Long tenantId,
            @Param("appId") String appId,
            @Param("bizRefundNo") String bizRefundNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderVO selectByTenantAndRefundOrderNo(
            @Param("tenantId") Long tenantId,
            @Param("refundOrderNo") String refundOrderNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderVO selectByTenantAndChannelRefundNo(
            @Param("tenantId") Long tenantId,
            @Param("channelRefundNo") String channelRefundNo);

    @InterceptorIgnore(tenantLine = "true")
    int updateRefundingQueryResult(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("status") String status,
            @Param("refundTime") LocalDateTime refundTime);

    @InterceptorIgnore(tenantLine = "true")
    Long sumOccupyingRefundAmount(
            @Param("tenantId") Long tenantId,
            @Param("paymentOrderId") Long paymentOrderId);

    @InterceptorIgnore(tenantLine = "true")
    String selectLatestFlowNo(
            @Param("tenantId") Long tenantId,
            @Param("refundOrderId") Long refundOrderId);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundOrderEntity selectEntityByTenantAndChannelRefundNo(
            @Param("tenantId") Long tenantId,
            @Param("channelRefundNo") String channelRefundNo);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentRefundOrderEntity> selectSuccessfulChannelRefundsMissingInBill(
            @Param("tenantId") Long tenantId,
            @Param("channelCode") String channelCode,
            @Param("billDate") LocalDate billDate,
            @Param("nextBillDate") LocalDate nextBillDate,
            @Param("channelRefundNos") List<String> channelRefundNos);
}
