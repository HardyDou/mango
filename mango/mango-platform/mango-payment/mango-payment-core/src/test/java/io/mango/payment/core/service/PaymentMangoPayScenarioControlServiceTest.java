package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreateMangoPayScenarioControlCommand;
import io.mango.payment.core.entity.PaymentChannel;
import io.mango.payment.core.entity.PaymentMangoPayScenarioControl;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.mapper.PaymentMangoPayScenarioControlMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMangoPayScenarioControlServiceTest {

    private PaymentMangoPayScenarioControlMapper scenarioControlMapper;
    private PaymentChannelMapper channelMapper;
    private PaymentChannelContractMapper channelContractMapper;
    private PaymentOperationAuditService auditService;
    private PaymentNumberService numberService;
    private PaymentMangoPayScenarioControlService service;

    @BeforeEach
    void setUp() {
        scenarioControlMapper = mock(PaymentMangoPayScenarioControlMapper.class);
        channelMapper = mock(PaymentChannelMapper.class);
        channelContractMapper = mock(PaymentChannelContractMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        numberService = mock(PaymentNumberService.class);
        when(numberService.next(PaymentNumberService.PAY_MANGO_SCENARIO_NO)).thenReturn("SC2026060600000001");
        service = new PaymentMangoPayScenarioControlService(
                scenarioControlMapper,
                channelMapper,
                channelContractMapper,
                new PaymentMangoPayResultMappingService(),
                auditService,
                numberService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("createScenarioControl should persist active next payment scenario and audit")
    void createScenarioControl_paymentScenario_persistsAndAudits() {
        when(channelMapper.selectOne(any())).thenReturn(mangoPayChannel());
        ArgumentCaptor<PaymentMangoPayScenarioControl> captor = ArgumentCaptor.forClass(PaymentMangoPayScenarioControl.class);
        CreateMangoPayScenarioControlCommand command = new CreateMangoPayScenarioControlCommand();
        command.setChannelCode("MANGO_PAY");
        command.setScenarioType("payment");
        command.setScenarioCode("fail");
        command.setEffectiveCount(1);
        command.setRemark("下一笔失败");

        service.createScenarioControl(command);

        verify(scenarioControlMapper).insert(captor.capture());
        PaymentMangoPayScenarioControl entity = captor.getValue();
        assertThat(entity.getControlNo()).startsWith("SC");
        assertThat(entity.getChannelCode()).isEqualTo("MANGO_PAY");
        assertThat(entity.getScenarioType()).isEqualTo("PAYMENT");
        assertThat(entity.getScenarioCode()).isEqualTo("FAIL");
        assertThat(entity.getEffectiveCount()).isEqualTo(1);
        assertThat(entity.getConsumedCount()).isZero();
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
        assertThat(entity.getTenantId()).isEqualTo(1L);
        verify(auditService).record(
                eq(PaymentOperationAuditService.ACTION_CREATE_MANGO_PAY_CHANNEL_SCENARIO),
                eq(PaymentOperationAuditService.RESOURCE_PAYMENT_MANGO_PAY_CHANNEL_SCENARIO),
                eq(entity.getControlNo()),
                eq(PaymentOperationAuditService.RESULT_SUCCESS));
    }

    @Test
    @DisplayName("createScenarioControl should reject non Mango Pay channel")
    void createScenarioControl_nonMangoPay_rejects() {
        CreateMangoPayScenarioControlCommand command = new CreateMangoPayScenarioControlCommand();
        command.setChannelCode("ALLINPAY");
        command.setScenarioType("PAYMENT");
        command.setScenarioCode("SUCCESS");
        command.setEffectiveCount(1);

        assertThatThrownBy(() -> service.createScenarioControl(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("芒果支付");
    }

    @Test
    @DisplayName("createScenarioControl should persist callback delay scenario")
    void createScenarioControl_callbackDelay_persistsDelayMinutes() {
        when(channelMapper.selectOne(any())).thenReturn(mangoPayChannel());
        ArgumentCaptor<PaymentMangoPayScenarioControl> captor = ArgumentCaptor.forClass(PaymentMangoPayScenarioControl.class);
        CreateMangoPayScenarioControlCommand command = new CreateMangoPayScenarioControlCommand();
        command.setChannelCode("MANGO_PAY");
        command.setScenarioType("CALLBACK_DELAY");
        command.setCallbackDelayMinutes(15);
        command.setEffectiveCount(1);

        service.createScenarioControl(command);

        verify(scenarioControlMapper).insert(captor.capture());
        PaymentMangoPayScenarioControl entity = captor.getValue();
        assertThat(entity.getScenarioType()).isEqualTo("CALLBACK_DELAY");
        assertThat(entity.getCallbackDelayMinutes()).isEqualTo(15);
        assertThat(entity.getScenarioCode()).isNull();
    }

    @Test
    @DisplayName("createScenarioControl should reject callback delay over one day")
    void createScenarioControl_callbackDelayTooLarge_rejects() {
        when(channelMapper.selectOne(any())).thenReturn(mangoPayChannel());
        CreateMangoPayScenarioControlCommand command = new CreateMangoPayScenarioControlCommand();
        command.setChannelCode("MANGO_PAY");
        command.setScenarioType("CALLBACK_DELAY");
        command.setCallbackDelayMinutes(1441);
        command.setEffectiveCount(1);

        assertThatThrownBy(() -> service.createScenarioControl(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("回调延迟分钟数");
    }

    @Test
    @DisplayName("consumePaymentScenario should consume next active scenario once")
    void consumePaymentScenario_activeScenario_returnsMappedResult() {
        PaymentMangoPayScenarioControl control = new PaymentMangoPayScenarioControl();
        control.setId(900001L);
        control.setScenarioCode("TIMEOUT");
        control.setEffectiveCount(1);
        control.setConsumedCount(0);
        control.setStatus("ACTIVE");
        when(scenarioControlMapper.selectNextActive(1L, "MANGO_PAY", 331001L, "PAYMENT_QUERY")).thenReturn(control);
        when(scenarioControlMapper.consume(1L, 900001L, 1001L)).thenReturn(1);

        PaymentMangoPayResultMappingService.PaymentChannelResult result =
                service.consumePaymentScenario(331001L, "PAYMENT_QUERY");

        assertThat(result.returnCode()).isEqualTo("TIMEOUT");
        assertThat(result.resultType()).isEqualTo("TIMEOUT");
        assertThat(result.status()).isEqualTo("PAYING");
        assertThat(control.getStatus()).isEqualTo("CONSUMED");
    }

    private PaymentChannel mangoPayChannel() {
        PaymentChannel channel = new PaymentChannel();
        channel.setId(330001L);
        channel.setTenantId(1L);
        channel.setChannelCode("MANGO_PAY");
        channel.setStatus(1);
        return channel;
    }
}
