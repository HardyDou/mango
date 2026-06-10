package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentOfflineRefundVO;
import io.mango.payment.core.entity.PaymentOfflineRefundEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentOfflineRefundMapper extends BaseMapper<PaymentOfflineRefundEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countOfflineRefunds(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOfflineRefundVO> selectOfflineRefundPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOfflineRefundVO selectOfflineRefundDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    Long sumRefundedAmountByCollection(
            @Param("tenantId") Long tenantId,
            @Param("offlineCollectionId") Long offlineCollectionId);
}
