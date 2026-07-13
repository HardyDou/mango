package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentOfflineRefundProjection;
import io.mango.payment.core.entity.PaymentOfflineRefundEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentOfflineRefundMapper extends BaseMapper<PaymentOfflineRefundEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countOfflineRefunds(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOfflineRefundProjection> selectOfflineRefundPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOfflineRefundProjection selectOfflineRefundDetail(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    Long sumRefundedAmountByCollection(
            @Param("tenantId") String tenantId,
            @Param("offlineCollectionId") Long offlineCollectionId);
}
