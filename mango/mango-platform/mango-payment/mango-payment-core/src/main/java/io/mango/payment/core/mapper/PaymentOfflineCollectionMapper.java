package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentOfflineCollectionProjection;
import io.mango.payment.core.entity.PaymentOfflineCollectionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentOfflineCollectionMapper extends BaseMapper<PaymentOfflineCollectionEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countOfflineCollections(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOfflineCollectionProjection> selectOfflineCollectionPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOfflineCollectionProjection selectOfflineCollectionDetail(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOfflineCollectionEntity selectByPayOrderNoForUpdate(
            @Param("tenantId") String tenantId,
            @Param("payOrderNo") String payOrderNo);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOfflineCollectionEntity selectEntityForUpdate(
            @Param("tenantId") String tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOfflineCollectionEntity selectByReconciliationCodeForUpdate(
            @Param("tenantId") String tenantId,
            @Param("reconciliationCode") String reconciliationCode);

    @InterceptorIgnore(tenantLine = "true")
    int submitTransferVoucher(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("transferAmount") Long transferAmount,
            @Param("voucherFileIds") String voucherFileIds,
            @Param("voucherCount") Integer voucherCount,
            @Param("submitRemark") String submitRemark);

    @InterceptorIgnore(tenantLine = "true")
    int confirmCollection(
            @Param("tenantId") String tenantId,
            @Param("id") Long id,
            @Param("confirmedAmount") Long confirmedAmount,
            @Param("confirmedTime") java.time.LocalDateTime confirmedTime,
            @Param("confirmedBy") Long confirmedBy,
            @Param("confirmedByName") String confirmedByName,
            @Param("confirmRemark") String confirmRemark,
            @Param("nextStatus") String nextStatus,
            @Param("requiredStatus") String requiredStatus);
}
