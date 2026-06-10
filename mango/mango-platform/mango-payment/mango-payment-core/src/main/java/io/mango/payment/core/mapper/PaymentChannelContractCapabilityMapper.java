package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentChannelCertificateExpiryVO;
import io.mango.payment.core.entity.PaymentChannelContractCapability;
import io.mango.payment.core.model.PaymentCashierRouteMatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentChannelContractCapabilityMapper extends BaseMapper<PaymentChannelContractCapability> {

    int deletePhysicallyByContractId(@Param("contractId") Long contractId, @Param("tenantId") Long tenantId);

    int deletePhysicallyById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    long countRouteRelations(@Param("tenantId") Long tenantId, @Param("contractCapabilityId") Long contractCapabilityId);

    PaymentChannelContractCapability selectRouteCapability(
            @Param("tenantId") Long tenantId,
            @Param("contractCapabilityId") Long contractCapabilityId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType,
            @Param("environment") String environment);

    PaymentCashierRouteMatch selectRoutedCashierCapability(
            @Param("tenantId") Long tenantId,
            @Param("applicationId") Long applicationId,
            @Param("subjectId") Long subjectId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType,
            @Param("amount") Long amount);

    PaymentCashierRouteMatch selectFallbackCashierCapability(
            @Param("tenantId") Long tenantId,
            @Param("subjectId") Long subjectId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType,
            @Param("amount") Long amount);

    long countFallbackDisabledRouteRules(
            @Param("tenantId") Long tenantId,
            @Param("applicationId") Long applicationId,
            @Param("subjectId") Long subjectId,
            @Param("methodCode") String methodCode,
            @Param("terminalType") String terminalType);

    List<PaymentChannelCertificateExpiryVO> selectExpiringCertificates(
            @Param("tenantId") Long tenantId,
            @Param("deadline") LocalDateTime deadline,
            @Param("now") LocalDateTime now);
}
