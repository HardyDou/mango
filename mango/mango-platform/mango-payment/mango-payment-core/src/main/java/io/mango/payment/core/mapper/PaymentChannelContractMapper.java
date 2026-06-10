package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentChannelContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentChannelContractMapper extends BaseMapper<PaymentChannelContract> {

    long countDeleteRelations(
            @Param("tenantId") Long tenantId,
            @Param("contractId") Long contractId,
            @Param("channelId") Long channelId,
            @Param("subjectId") Long subjectId);

    String selectActiveConfigValuesJson(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId);
}
