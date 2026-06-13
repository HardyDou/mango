package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentOfflineCollectionVoucherEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface PaymentOfflineCollectionVoucherMapper extends BaseMapper<PaymentOfflineCollectionVoucherEntity> {

    int acceptByCollection(
            @Param("tenantId") Long tenantId,
            @Param("offlineCollectionId") Long offlineCollectionId,
            @Param("updatedBy") Long updatedBy,
            @Param("updatedAt") LocalDateTime updatedAt);
}
