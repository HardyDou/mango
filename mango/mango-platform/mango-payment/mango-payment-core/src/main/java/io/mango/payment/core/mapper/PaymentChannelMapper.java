package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentChannelMapper extends BaseMapper<PaymentChannel> {

    long countDeleteRelations(@Param("tenantId") Long tenantId, @Param("channelId") Long channelId, @Param("channelCode") String channelCode);
}
