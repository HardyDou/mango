package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentChannelContractValueEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentChannelContractValueMapper extends BaseMapper<PaymentChannelContractValueEntity> {

    int deletePhysicallyByContractId(@Param("contractId") Long contractId, @Param("tenantId") Long tenantId);
}
