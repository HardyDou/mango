package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentMethodConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMethodConfigMapper extends BaseMapper<PaymentMethodConfig> {
}
