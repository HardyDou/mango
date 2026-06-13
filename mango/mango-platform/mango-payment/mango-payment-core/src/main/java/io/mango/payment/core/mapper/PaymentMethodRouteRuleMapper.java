package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentMethodRouteRule;
import io.mango.payment.core.model.PaymentMethodRouteCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMethodRouteRuleMapper extends BaseMapper<PaymentMethodRouteRule> {

    long countDeleteRelations(@Param("tenantId") Long tenantId, @Param("routeRuleId") Long routeRuleId);

    List<PaymentMethodRouteCandidate> selectRouteCandidates(
            @Param("tenantId") Long tenantId,
            @Param("applicationId") Long applicationId,
            @Param("subjectId") Long subjectId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType,
            @Param("environment") String environment);
}
