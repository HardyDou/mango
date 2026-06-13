package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentMangoPayScenarioControl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMangoPayScenarioControlMapper extends BaseMapper<PaymentMangoPayScenarioControl> {

    @InterceptorIgnore(tenantLine = "true")
    PaymentMangoPayScenarioControl selectNextActive(
            @Param("tenantId") Long tenantId,
            @Param("channelCode") String channelCode,
            @Param("contractId") Long contractId,
            @Param("scenarioType") String scenarioType);

    @InterceptorIgnore(tenantLine = "true")
    int consume(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("operatorId") Long operatorId);
}
