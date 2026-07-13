package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentMangoPayScenarioControlEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMangoPayScenarioControlMapper extends BaseMapper<PaymentMangoPayScenarioControlEntity> {

    @InterceptorIgnore(tenantLine = "true")
    PaymentMangoPayScenarioControlEntity selectNextActive(
            @Param("tenantId") String tenantId,
            @Param("channelCode") String channelCode,
            @Param("contractId") Long contractId,
            @Param("scenarioType") String scenarioType);

    @InterceptorIgnore(tenantLine = "true")
    int consume(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("operatorId") Long operatorId);
}
