package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentMethodRouteRuleItemVO;
import io.mango.payment.core.entity.PaymentMethodRouteRuleItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMethodRouteRuleItemMapper extends BaseMapper<PaymentMethodRouteRuleItem> {

    int deleteByRuleId(@Param("ruleId") Long ruleId, @Param("tenantId") Long tenantId);

    List<PaymentMethodRouteRuleItemVO> selectItemsByRuleId(@Param("ruleId") Long ruleId, @Param("tenantId") Long tenantId);
}
