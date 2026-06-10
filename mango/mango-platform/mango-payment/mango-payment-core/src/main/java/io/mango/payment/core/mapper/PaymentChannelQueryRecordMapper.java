package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentChannelQueryRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentChannelQueryRecordMapper extends BaseMapper<PaymentChannelQueryRecordEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countByTenantAndPayOrderNo(
            @Param("tenantId") Long tenantId,
            @Param("payOrderNo") String payOrderNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentChannelQueryRecordEntity selectLastByTenantAndPayOrderNo(
            @Param("tenantId") Long tenantId,
            @Param("payOrderNo") String payOrderNo);
}
