package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentChannelBillSourceVO;
import io.mango.payment.core.entity.PaymentChannelBillSourceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentChannelBillSourceMapper extends BaseMapper<PaymentChannelBillSourceEntity> {

    long countBillSources(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("contractId") Long contractId);

    List<PaymentChannelBillSourceVO> selectBillSourcePage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("contractId") Long contractId,
            @Param("limit") long limit,
            @Param("offset") long offset);

    PaymentChannelBillSourceVO selectBillSourceDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    List<PaymentChannelBillSourceEntity> selectEnabledAutomaticSources(@Param("tenantId") Long tenantId);
}
