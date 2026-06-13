package io.mango.payment.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentChannelCapabilityCommand;
import io.mango.payment.api.command.SavePaymentChannelCommand;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.core.entity.PaymentChannel;
import io.mango.payment.core.entity.PaymentChannelCapability;
import io.mango.payment.core.mapper.PaymentChannelCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.service.PaymentOperationAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentChannelServiceImplTest {

    private PaymentChannelMapper channelMapper;
    private PaymentChannelCapabilityMapper capabilityMapper;
    private PaymentOperationAuditService auditService;
    private PaymentChannelServiceImpl service;

    @BeforeEach
    void setUp() {
        channelMapper = mock(PaymentChannelMapper.class);
        capabilityMapper = mock(PaymentChannelCapabilityMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        service = new PaymentChannelServiceImpl(channelMapper, capabilityMapper, auditService, new ObjectMapper());
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        when(capabilityMapper.selectList(any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("createChannel should save channel capabilities and audit success")
    void createChannel_savesCapabilitiesAndAudits() {
        SavePaymentChannelCommand command = command();

        service.createChannel(command);

        ArgumentCaptor<PaymentChannel> channelCaptor = ArgumentCaptor.forClass(PaymentChannel.class);
        ArgumentCaptor<PaymentChannelCapability> capabilityCaptor = ArgumentCaptor.forClass(PaymentChannelCapability.class);
        verify(channelMapper).insert(channelCaptor.capture());
        verify(capabilityMapper).insert(capabilityCaptor.capture());
        assertThat(channelCaptor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(channelCaptor.getValue().getChannelCode()).isEqualTo("LIANLIAN_PAY");
        assertThat(capabilityCaptor.getValue().getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(capabilityCaptor.getValue().getTerminalType()).isEqualTo("WEB");
        assertThat(channelCaptor.getValue().getEnvironment()).isEqualTo("PROD");
        assertThat(capabilityCaptor.getValue().getEnvironment()).isEqualTo("PROD");
        assertThat(capabilityCaptor.getValue().getMinAmount()).isEqualTo(1L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CREATE_CHANNEL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL,
                "LIANLIAN_PAY",
                PaymentOperationAuditService.RESULT_SUCCESS);
        assertThat(channelCaptor.getValue().getBillFetchModes()).isEqualTo("MANUAL,FTP");
    }

    @Test
    @DisplayName("createChannel should reject invalid sensitive field template")
    void createChannel_rejectsInvalidSensitiveFieldTemplate() {
        SavePaymentChannelCommand command = command();
        command.setFieldTemplateJson("""
                [{"name":"privateKey","label":"商户私钥","component":"textarea","dataType":"string","sensitive":true}]
                """);

        assertThatThrownBy(() -> service.createChannel(command))
                .isInstanceOf(BizException.class)
                .hasMessage("商户私钥敏感字段必须声明加密或脱敏");

        verify(channelMapper, never()).insert(any(PaymentChannel.class));
    }

    @Test
    @DisplayName("updateChannel should reject removing capability when contract or route references it")
    void updateChannel_rejectsRemovingReferencedCapability() {
        PaymentChannel channel = channel();
        PaymentChannelCapability existing = capability();
        when(channelMapper.selectById(330009L)).thenReturn(channel);
        when(capabilityMapper.selectList(any())).thenReturn(List.of(existing));
        when(capabilityMapper.countDeleteRelations(1L, 332009L)).thenReturn(1L);
        SavePaymentChannelCommand command = command();
        command.setId(330009L);
        command.setCapabilities(List.of());

        assertThatThrownBy(() -> service.updateChannel(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("通道能力存在签约或路由引用");

        verify(capabilityMapper, never()).deletePhysicallyById(332009L, 1L);
    }

    @Test
    @DisplayName("deleteChannel should reject and audit when channel has relations")
    void deleteChannel_withRelations_rejectsAndAudits() {
        PaymentChannel channel = channel();
        when(channelMapper.selectById(330009L)).thenReturn(channel);
        when(channelMapper.countDeleteRelations(1L, 330009L, "LIANLIAN_PAY")).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteChannel(330009L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_CHANNEL_DELETE_HAS_RELATIONS.getMessage());

        verify(channelMapper, never()).deleteById(330009L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_CHANNEL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL,
                "LIANLIAN_PAY",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("deleteChannel should reject builtin channels before relation cleanup")
    void deleteChannel_builtinChannel_rejectsAndAudits() {
        PaymentChannel channel = channel();
        channel.setChannelCode("OFFLINE_COLLECTION");
        channel.setChannelType("BUILTIN_OFFLINE");
        channel.setAdapterType("OFFLINE_COLLECTION");
        when(channelMapper.selectById(330009L)).thenReturn(channel);

        assertThatThrownBy(() -> service.deleteChannel(330009L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_CHANNEL_BUILTIN_DELETE_FORBIDDEN.getMessage());

        verify(channelMapper, never()).countDeleteRelations(any(), any(), any());
        verify(channelMapper, never()).deleteById(330009L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_CHANNEL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL,
                "OFFLINE_COLLECTION",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("deleteChannel should physically delete child capabilities and audit when no relations exist")
    void deleteChannel_withoutRelations_deletesChildrenAndAudits() {
        PaymentChannel channel = channel();
        PaymentChannelCapability existing = capability();
        when(channelMapper.selectById(330009L)).thenReturn(channel);
        when(capabilityMapper.selectList(any())).thenReturn(List.of(existing));
        when(channelMapper.countDeleteRelations(1L, 330009L, "LIANLIAN_PAY")).thenReturn(0L);
        when(channelMapper.deleteById(330009L)).thenReturn(1);

        service.deleteChannel(330009L);

        verify(capabilityMapper).deletePhysicallyById(332009L, 1L);
        verify(channelMapper).deleteById(330009L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_CHANNEL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL,
                "LIANLIAN_PAY",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    private SavePaymentChannelCommand command() {
        SavePaymentChannelCommand command = new SavePaymentChannelCommand();
        command.setChannelCode(PaymentChannelCode.LIANLIAN_PAY);
        command.setChannelName("连连支付通道");
        command.setChannelType("AGGREGATOR");
        command.setAdapterType("LIANLIAN_PAY");
        command.setGatewayBaseUrl("https://openapi.lianlianpay.com");
        command.setFieldTemplateJson("""
                [{"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true}]
                """);
        command.setCapabilitySummary("微信扫码、退款、查单、账单、对账");
        command.setBillFetchModes(List.of("MANUAL", "FTP"));
        command.setCapabilities(List.of(capabilityCommand()));
        command.setStatus(1);
        return command;
    }

    private SavePaymentChannelCapabilityCommand capabilityCommand() {
        SavePaymentChannelCapabilityCommand command = new SavePaymentChannelCapabilityCommand();
        command.setMethodCode("PERSONAL_WECHAT_QR");
        command.setTerminalType("WEB");
        command.setSupportsRefund(1);
        command.setSupportsQuery(1);
        command.setSupportsClose(1);
        command.setSupportsBill(1);
        command.setSupportsReconcile(1);
        command.setMinAmount(1L);
        command.setMaxAmount(999999L);
        command.setStatus(1);
        return command;
    }

    private PaymentChannel channel() {
        PaymentChannel channel = new PaymentChannel();
        channel.setId(330009L);
        channel.setTenantId(1L);
        channel.setChannelCode("LIANLIAN_PAY");
        channel.setChannelName("连连支付通道");
        channel.setChannelType("AGGREGATOR");
        channel.setAdapterType("LIANLIAN_PAY");
        channel.setStatus(1);
        return channel;
    }

    private PaymentChannelCapability capability() {
        PaymentChannelCapability capability = new PaymentChannelCapability();
        capability.setId(332009L);
        capability.setTenantId(1L);
        capability.setChannelId(330009L);
        capability.setMethodCode("PERSONAL_WECHAT_QR");
        capability.setTerminalType("WEB");
        capability.setEnvironment("PROD");
        capability.setStatus(1);
        return capability;
    }
}
