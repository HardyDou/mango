package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentReconciliationVO;
import io.mango.payment.core.entity.PaymentReconciliationEntity;
import io.mango.payment.core.model.PaymentChannelBillItemRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PaymentReconciliationMapper extends BaseMapper<PaymentReconciliationEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countReconciliations(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentReconciliationVO> selectReconciliationPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentReconciliationVO selectReconciliationDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    long countImportedFile(
            @Param("tenantId") Long tenantId,
            @Param("channelCode") String channelCode,
            @Param("billDate") LocalDate billDate,
            @Param("fileDigest") String fileDigest);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentChannelBillItemRow> selectMangoPayBillItems(
            @Param("tenantId") Long tenantId,
            @Param("channelCode") String channelCode,
            @Param("billDate") LocalDate billDate,
            @Param("nextBillDate") LocalDate nextBillDate);
}
