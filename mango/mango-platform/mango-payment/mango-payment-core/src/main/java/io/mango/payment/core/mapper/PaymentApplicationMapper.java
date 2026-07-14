package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentApplicationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentApplicationMapper extends BaseMapper<PaymentApplicationEntity> {

    long countDeleteRelations(
            @Param("tenantId") String tenantId,
            @Param("applicationId") Long applicationId,
            @Param("appId") String appId,
            @Param("legacyAppCode") String legacyAppCode);
}
