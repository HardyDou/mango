package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentChannelBillSourceProjection;
import io.mango.payment.core.entity.PaymentChannelBillSourceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentChannelBillSourceMapper extends BaseMapper<PaymentChannelBillSourceEntity> {

    long countBillSources(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("contractId") Long contractId);

    List<PaymentChannelBillSourceProjection> selectBillSourcePage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("contractId") Long contractId,
            @Param("limit") long limit,
            @Param("offset") long offset);

    PaymentChannelBillSourceProjection selectBillSourceDetail(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    List<PaymentChannelBillSourceEntity> selectEnabledAutomaticSources(@Param("tenantId") String tenantId);
}
