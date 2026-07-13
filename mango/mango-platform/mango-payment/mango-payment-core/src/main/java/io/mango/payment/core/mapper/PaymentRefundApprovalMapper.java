package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentRefundApprovalProjection;
import io.mango.payment.core.entity.PaymentRefundApprovalEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentRefundApprovalMapper extends BaseMapper<PaymentRefundApprovalEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countRefundApprovals(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentRefundApprovalProjection> selectRefundApprovalPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundApprovalProjection selectRefundApprovalDetail(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundApprovalEntity selectEntityForUpdate(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundApprovalEntity selectEntityByApprovalNoForUpdate(
            @Param("tenantId") String tenantId,
            @Param("approvalNo") String approvalNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundApprovalEntity selectEntityByBizRefundNoForUpdate(
            @Param("tenantId") String tenantId,
            @Param("appId") String appId,
            @Param("bizRefundNo") String bizRefundNo);

    @InterceptorIgnore(tenantLine = "true")
    Long sumPendingApprovalAmount(
            @Param("tenantId") String tenantId,
            @Param("paymentOrderId") Long paymentOrderId);
}
