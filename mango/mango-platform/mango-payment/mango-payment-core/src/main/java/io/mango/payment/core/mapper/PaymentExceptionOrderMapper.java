package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentExceptionOrderProjection;
import io.mango.payment.core.entity.PaymentExceptionOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentExceptionOrderMapper extends BaseMapper<PaymentExceptionOrderEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countExceptionOrders(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    long countCallbackFailureExceptionOrders(@Param("tenantId") String tenantId);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentExceptionOrderProjection> selectExceptionOrderPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentExceptionOrderProjection selectExceptionOrderDetail(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentExceptionOrderEntity selectActiveByBusinessKey(
            @Param("tenantId") String tenantId,
            @Param("relatedOrderNo") String relatedOrderNo,
            @Param("exceptionType") String exceptionType);

    @InterceptorIgnore(tenantLine = "true")
    int handleExceptionOrder(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("handleStatus") String handleStatus,
            @Param("handleAction") String handleAction,
            @Param("handleReason") String handleReason,
            @Param("handleResult") String handleResult,
            @Param("handleEvidence") String handleEvidence,
            @Param("handlerId") Long handlerId,
            @Param("handlerName") String handlerName,
            @Param("handleTime") LocalDateTime handleTime);
}
