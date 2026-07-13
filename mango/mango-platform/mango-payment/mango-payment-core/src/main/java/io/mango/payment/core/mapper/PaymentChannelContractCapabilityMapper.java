package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentChannelCertificateExpiryProjection;
import io.mango.payment.core.entity.PaymentChannelContractCapabilityEntity;
import io.mango.payment.core.model.PaymentCashierRouteMatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentChannelContractCapabilityMapper extends BaseMapper<PaymentChannelContractCapabilityEntity> {

    int deletePhysicallyByContractId(@Param("contractId") Long contractId, @Param("tenantId") String tenantId);

    int deletePhysicallyById(@Param("id") Long id, @Param("tenantId") String tenantId);

    long countRouteRelations(@Param("tenantId") String tenantId, @Param("contractCapabilityId") Long contractCapabilityId);

    PaymentChannelContractCapabilityEntity selectRouteCapability(
            @Param("tenantId") String tenantId,
            @Param("contractCapabilityId") Long contractCapabilityId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType,
            @Param("environment") String environment);

    String selectRouteEnvironment(
            @Param("tenantId") String tenantId,
            @Param("contractCapabilityId") Long contractCapabilityId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType);

    PaymentCashierRouteMatch selectRoutedCashierCapability(
            @Param("tenantId") String tenantId,
            @Param("applicationId") Long applicationId,
            @Param("subjectId") Long subjectId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType,
            @Param("amount") Long amount);

    PaymentCashierRouteMatch selectFallbackCashierCapability(
            @Param("tenantId") String tenantId,
            @Param("subjectId") Long subjectId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType,
            @Param("amount") Long amount);

    long countFallbackDisabledRouteRules(
            @Param("tenantId") String tenantId,
            @Param("applicationId") Long applicationId,
            @Param("subjectId") Long subjectId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType);

    List<PaymentChannelCertificateExpiryProjection> selectExpiringCertificates(
            @Param("tenantId") String tenantId,
            @Param("deadline") LocalDateTime deadline,
            @Param("now") LocalDateTime now);
}
