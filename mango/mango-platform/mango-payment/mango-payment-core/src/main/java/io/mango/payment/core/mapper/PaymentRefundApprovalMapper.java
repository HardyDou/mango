package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentRefundApprovalVO;
import io.mango.payment.core.entity.PaymentRefundApprovalEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentRefundApprovalMapper extends BaseMapper<PaymentRefundApprovalEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countRefundApprovals(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentRefundApprovalVO> selectRefundApprovalPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundApprovalVO selectRefundApprovalDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundApprovalEntity selectEntityForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundApprovalEntity selectEntityByApprovalNoForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("approvalNo") String approvalNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundApprovalEntity selectEntityByBizRefundNoForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("appId") String appId,
            @Param("bizRefundNo") String bizRefundNo);

    @InterceptorIgnore(tenantLine = "true")
    Long sumPendingApprovalAmount(
            @Param("tenantId") Long tenantId,
            @Param("paymentOrderId") Long paymentOrderId);
}
