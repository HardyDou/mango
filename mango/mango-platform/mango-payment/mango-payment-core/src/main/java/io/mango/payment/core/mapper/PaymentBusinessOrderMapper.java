package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentBusinessOrderVO;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentBusinessOrderMapper extends BaseMapper<PaymentBusinessOrderEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countBusinessOrders(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("applicationId") Long applicationId,
            @Param("enterpriseSubjectId") Long enterpriseSubjectId);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentBusinessOrderVO> selectBusinessOrderPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("applicationId") Long applicationId,
            @Param("enterpriseSubjectId") Long enterpriseSubjectId,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentBusinessOrderVO selectBusinessOrderDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    PaymentBusinessOrderEntity selectCashierBusinessOrder(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    List<PaymentBusinessOrderEntity> selectLatestPayableCashierOrder(
            @Param("tenantId") Long tenantId,
            @Param("appId") String appId,
            @Param("legacyAppCode") String legacyAppCode,
            @Param("subjectIds") List<Long> subjectIds);

    int markCashierPaySuccess(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("paidAmount") Long paidAmount);

    int touchCashierPayingOrder(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    int closeOpenBusinessOrder(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    int updateRefundProgress(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("refundAmount") Long refundAmount);
}
