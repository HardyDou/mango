package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentCashierConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentCashierConfigMapper extends BaseMapper<PaymentCashierConfig> {

    @InterceptorIgnore(tenantLine = "true")
    PaymentCashierConfig selectByIdIgnoreTenant(@Param("id") Long id);

    long countDeleteRelations(@Param("tenantId") Long tenantId, @Param("cashierConfigId") Long cashierConfigId);
}
