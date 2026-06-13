package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMethodMapper extends BaseMapper<PaymentMethod> {

    long countDeleteRelations(@Param("tenantId") Long tenantId, @Param("methodId") Long methodId, @Param("methodCode") String methodCode);
}
