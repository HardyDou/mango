package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentVirtualChannelPaymentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentVirtualChannelPaymentMapper extends BaseMapper<PaymentVirtualChannelPaymentEntity> {

    PaymentVirtualChannelPaymentEntity selectByTenantAndPayOrderNo(
            @Param("tenantId") String tenantId,
            @Param("payOrderNo") String payOrderNo);
}
