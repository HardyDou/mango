package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentChannelContractEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentChannelContractMapper extends BaseMapper<PaymentChannelContractEntity> {

    long countDeleteRelations(
            @Param("tenantId") String tenantId,
            @Param("contractId") Long contractId);

    String selectActiveConfigValuesJson(@Param("tenantId") String tenantId, @Param("contractId") Long contractId);
}
