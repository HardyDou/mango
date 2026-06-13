package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentNotificationRecordVO;
import io.mango.payment.core.entity.PaymentNotificationRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentNotificationRecordMapper extends BaseMapper<PaymentNotificationRecordEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countNotificationRecords(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    long countFailedNotificationRecords(@Param("tenantId") Long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentNotificationRecordVO> selectNotificationRecordPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentNotificationRecordVO selectNotificationRecordDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentNotificationRecordEntity> selectDueNotificationRecords(
            @Param("tenantId") Long tenantId,
            @Param("now") LocalDateTime now,
            @Param("limit") long limit);

    @InterceptorIgnore(tenantLine = "true")
    List<Long> selectDueNotificationTenantIds(
            @Param("now") LocalDateTime now,
            @Param("limit") long limit);

    @InterceptorIgnore(tenantLine = "true")
    int claimDueNotificationRecord(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("now") LocalDateTime now,
            @Param("operatorId") Long operatorId);

    @InterceptorIgnore(tenantLine = "true")
    int updateDeliveryResult(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("notifyStatus") String notifyStatus,
            @Param("responseCode") String responseCode,
            @Param("responseMessage") String responseMessage,
            @Param("nextRetryTime") LocalDateTime nextRetryTime,
            @Param("now") LocalDateTime now,
            @Param("operatorId") Long operatorId);

    @InterceptorIgnore(tenantLine = "true")
    int manualRetryNotificationRecord(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("retryReason") String retryReason,
            @Param("retryResult") String retryResult,
            @Param("now") LocalDateTime now,
            @Param("operatorId") Long operatorId,
            @Param("operatorName") String operatorName);
}
