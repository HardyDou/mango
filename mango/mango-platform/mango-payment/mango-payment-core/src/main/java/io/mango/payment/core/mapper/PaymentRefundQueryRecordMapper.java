package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentRefundQueryRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentRefundQueryRecordMapper extends BaseMapper<PaymentRefundQueryRecordEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countByTenantAndRefundOrderNo(
            @Param("tenantId") Long tenantId,
            @Param("refundOrderNo") String refundOrderNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentRefundQueryRecordEntity selectLastByTenantAndRefundOrderNo(
            @Param("tenantId") Long tenantId,
            @Param("refundOrderNo") String refundOrderNo);
}
