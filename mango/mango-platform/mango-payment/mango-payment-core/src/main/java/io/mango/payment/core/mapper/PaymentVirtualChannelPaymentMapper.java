package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentVirtualChannelPayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentVirtualChannelPaymentMapper extends BaseMapper<PaymentVirtualChannelPayment> {

    PaymentVirtualChannelPayment selectByTenantAndPayOrderNo(
            @Param("tenantId") Long tenantId,
            @Param("payOrderNo") String payOrderNo);
}
