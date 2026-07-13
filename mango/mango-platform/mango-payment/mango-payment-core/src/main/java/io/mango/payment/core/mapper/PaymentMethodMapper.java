package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentMethodEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMethodMapper extends BaseMapper<PaymentMethodEntity> {

    long countDeleteRelations(@Param("tenantId") String tenantId, @Param("methodId") Long methodId, @Param("methodCode") String methodCode);
}
