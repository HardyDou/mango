package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentOfflineCollectionMatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface PaymentOfflineCollectionMatchMapper extends BaseMapper<PaymentOfflineCollectionMatchEntity> {

    @InterceptorIgnore(tenantLine = "true")
    int markConfirmed(
            @Param("tenantId") Long tenantId,
            @Param("bankStatementItemId") Long bankStatementItemId,
            @Param("confirmedTime") LocalDateTime confirmedTime,
            @Param("confirmedBy") Long confirmedBy,
            @Param("confirmedByName") String confirmedByName);
}
