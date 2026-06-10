package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentTransactionFlowVO;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentTransactionFlowMapper extends BaseMapper<PaymentTransactionFlowEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countTransactionFlows(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentTransactionFlowVO> selectTransactionFlowPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentTransactionFlowVO selectTransactionFlowDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentTransactionFlowEntity selectChannelFeeFlowByPaymentOrder(
            @Param("tenantId") Long tenantId,
            @Param("paymentOrderId") Long paymentOrderId);

    @InterceptorIgnore(tenantLine = "true")
    PaymentTransactionFlowEntity selectChannelFeeFlowByRefundOrder(
            @Param("tenantId") Long tenantId,
            @Param("refundOrderId") Long refundOrderId);
}
