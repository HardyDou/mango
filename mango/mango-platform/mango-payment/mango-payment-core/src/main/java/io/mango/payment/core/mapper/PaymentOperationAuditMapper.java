package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentOperationAuditVO;
import io.mango.payment.core.entity.PaymentOperationAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentOperationAuditMapper extends BaseMapper<PaymentOperationAudit> {

    @InterceptorIgnore(tenantLine = "true")
    long countOperationAudits(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("operationResult") String operationResult);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOperationAuditVO> selectOperationAuditPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("operationResult") String operationResult,
            @Param("limit") long limit,
            @Param("offset") long offset);
}
