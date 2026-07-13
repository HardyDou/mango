package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentOperationAuditProjection;
import io.mango.payment.core.entity.PaymentOperationAuditEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentOperationAuditMapper extends BaseMapper<PaymentOperationAuditEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countOperationAudits(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("operationResult") String operationResult);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOperationAuditProjection> selectOperationAuditPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("operationResult") String operationResult,
            @Param("limit") long limit,
            @Param("offset") long offset);
}
