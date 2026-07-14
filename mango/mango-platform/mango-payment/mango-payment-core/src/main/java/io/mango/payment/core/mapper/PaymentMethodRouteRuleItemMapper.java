package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentMethodRouteRuleItemProjection;
import io.mango.payment.core.entity.PaymentMethodRouteRuleItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMethodRouteRuleItemMapper extends BaseMapper<PaymentMethodRouteRuleItemEntity> {

    int deleteByRuleId(@Param("ruleId") Long ruleId, @Param("tenantId") String tenantId);

    List<PaymentMethodRouteRuleItemProjection> selectItemsByRuleId(@Param("ruleId") Long ruleId, @Param("tenantId") String tenantId);
}
