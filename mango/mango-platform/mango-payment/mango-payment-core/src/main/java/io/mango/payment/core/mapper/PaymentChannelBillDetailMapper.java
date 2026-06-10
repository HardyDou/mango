package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentChannelBillDetailVO;
import io.mango.payment.core.entity.PaymentChannelBillDetailEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentChannelBillDetailMapper extends BaseMapper<PaymentChannelBillDetailEntity> {

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentChannelBillDetailVO> selectBillDetails(
            @Param("tenantId") Long tenantId,
            @Param("reconciliationId") Long reconciliationId);
}
