package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentMethodCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMethodCategoryMapper extends BaseMapper<PaymentMethodCategory> {
}
