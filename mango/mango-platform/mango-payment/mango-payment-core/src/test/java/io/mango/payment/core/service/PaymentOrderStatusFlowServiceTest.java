package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.core.entity.PaymentOrderStatusFlowEntity;
import io.mango.payment.core.mapper.PaymentOrderStatusFlowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentOrderStatusFlowServiceTest {

    private PaymentOrderStatusFlowMapper statusFlowMapper;
    private PaymentOrderStatusFlowService service;

    @BeforeEach
    void setUp() {
        statusFlowMapper = mock(PaymentOrderStatusFlowMapper.class);
        service = new PaymentOrderStatusFlowService(statusFlowMapper);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("record should persist audit-aware status transition")
    void record_validTransition_persistsStatusFlow() {
        LocalDateTime happenTime = LocalDateTime.of(2026, 6, 6, 10, 30);
        ArgumentCaptor<PaymentOrderStatusFlowEntity> captor = ArgumentCaptor.forClass(PaymentOrderStatusFlowEntity.class);

        service.record(
                1L,
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                370001L,
                "PO202606060001",
                "PAYING",
                "SUCCESS",
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                "CH202606060001",
                happenTime,
                "通道回调成功");

        verify(statusFlowMapper).insert(captor.capture());
        PaymentOrderStatusFlowEntity entity = captor.getValue();
        assertThat(entity.getOrderType()).isEqualTo(PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT);
        assertThat(entity.getOrderNo()).isEqualTo("PO202606060001");
        assertThat(entity.getFromStatus()).isEqualTo("PAYING");
        assertThat(entity.getToStatus()).isEqualTo("SUCCESS");
        assertThat(entity.getTriggerSource()).isEqualTo(PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK);
        assertThat(entity.getTriggerNo()).isEqualTo("CH202606060001");
        assertThat(entity.getOperatorId()).isEqualTo(1001L);
        assertThat(entity.getOperatorName()).isEqualTo("admin");
        assertThat(entity.getHappenTime()).isEqualTo(happenTime);
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getDelFlag()).isZero();
    }

    @Test
    @DisplayName("record should reject missing target status")
    void record_missingTargetStatus_rejects() {
        assertThatThrownBy(() -> service.record(
                1L,
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                370001L,
                "PO202606060001",
                "PAYING",
                "",
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                "CH202606060001",
                LocalDateTime.now(),
                "通道回调成功"))
                .isInstanceOf(BizException.class);
    }
}
