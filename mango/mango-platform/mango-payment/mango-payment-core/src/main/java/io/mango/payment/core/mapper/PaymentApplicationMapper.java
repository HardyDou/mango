package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentApplicationMapper extends BaseMapper<PaymentApplication> {

    long countDeleteRelations(
            @Param("tenantId") Long tenantId,
            @Param("applicationId") Long applicationId,
            @Param("appId") String appId,
            @Param("legacyAppCode") String legacyAppCode);
}
