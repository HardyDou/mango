package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentTransactionFlowProjection;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentTransactionFlowMapper extends BaseMapper<PaymentTransactionFlowEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countTransactionFlows(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentTransactionFlowProjection> selectTransactionFlowPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentTransactionFlowProjection selectTransactionFlowDetail(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentTransactionFlowEntity selectChannelFeeFlowByPaymentOrder(
            @Param("tenantId") String tenantId,
            @Param("paymentOrderId") Long paymentOrderId);

    @InterceptorIgnore(tenantLine = "true")
    PaymentTransactionFlowEntity selectChannelFeeFlowByRefundOrder(
            @Param("tenantId") String tenantId,
            @Param("refundOrderId") Long refundOrderId);
}
