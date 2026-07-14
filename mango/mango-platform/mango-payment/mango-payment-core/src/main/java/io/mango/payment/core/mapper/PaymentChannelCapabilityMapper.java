package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentChannelCapabilityProjection;
import io.mango.payment.core.entity.PaymentChannelCapabilityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentChannelCapabilityMapper extends BaseMapper<PaymentChannelCapabilityEntity> {

    int deletePhysicallyById(@Param("id") Long id, @Param("tenantId") String tenantId);

    long countDeleteRelations(@Param("tenantId") String tenantId, @Param("capabilityId") Long capabilityId);

    long countChannelCapabilities(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("channelId") Long channelId);

    List<PaymentChannelCapabilityProjection> selectChannelCapabilityPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("channelId") Long channelId,
            @Param("limit") long limit,
            @Param("offset") long offset);

    PaymentChannelCapabilityProjection selectChannelCapabilityDetail(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);
}
