package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentChannelCapabilityVO;
import io.mango.payment.core.entity.PaymentChannelCapability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentChannelCapabilityMapper extends BaseMapper<PaymentChannelCapability> {

    int deletePhysicallyById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    long countDeleteRelations(@Param("tenantId") Long tenantId, @Param("capabilityId") Long capabilityId);

    long countChannelCapabilities(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("channelId") Long channelId);

    List<PaymentChannelCapabilityVO> selectChannelCapabilityPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("channelId") Long channelId,
            @Param("limit") long limit,
            @Param("offset") long offset);

    PaymentChannelCapabilityVO selectChannelCapabilityDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);
}
