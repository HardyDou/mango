package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentChannelEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentChannelMapper extends BaseMapper<PaymentChannelEntity> {

    long countDeleteRelations(@Param("tenantId") String tenantId, @Param("channelId") Long channelId, @Param("channelCode") String channelCode);
}
