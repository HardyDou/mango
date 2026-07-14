package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentCashierConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentCashierConfigMapper extends BaseMapper<PaymentCashierConfigEntity> {

    @InterceptorIgnore(tenantLine = "true")
    PaymentCashierConfigEntity selectByIdIgnoreTenant(@Param("id") Long id);

    long countDeleteRelations(@Param("tenantId") String tenantId, @Param("cashierConfigId") Long cashierConfigId);
}
