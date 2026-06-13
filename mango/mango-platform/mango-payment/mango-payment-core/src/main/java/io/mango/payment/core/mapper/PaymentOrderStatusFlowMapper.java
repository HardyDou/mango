package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentOrderStatusFlowVO;
import io.mango.payment.core.entity.PaymentOrderStatusFlowEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentOrderStatusFlowMapper extends BaseMapper<PaymentOrderStatusFlowEntity> {

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOrderStatusFlowVO> selectStatusFlows(
            @Param("tenantId") Long tenantId,
            @Param("orderType") String orderType,
            @Param("orderId") Long orderId);
}
