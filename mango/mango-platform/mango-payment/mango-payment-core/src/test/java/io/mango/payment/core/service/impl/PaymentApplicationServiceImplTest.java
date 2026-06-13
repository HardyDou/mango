package io.mango.payment.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreatePaymentApplicationCommand;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.service.PaymentOperationAuditService;
import io.mango.payment.core.service.PaymentSensitiveValueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentApplicationServiceImplTest {

    private PaymentApplicationMapper applicationMapper;
    private PaymentOperationAuditService auditService;
    private PaymentSensitiveValueService sensitiveValueService;
    private PaymentApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        applicationMapper = mock(PaymentApplicationMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        sensitiveValueService = mock(PaymentSensitiveValueService.class);
        service = new PaymentApplicationServiceImpl(applicationMapper, auditService, sensitiveValueService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @Test
    @DisplayName("createApplication should persist encrypted app secret and return plaintext only once")
    void createApplication_payloadEncryptionEnabled_encryptsStoredSecret() {
        when(applicationMapper.selectCount(any())).thenReturn(0L);
        when(sensitiveValueService.encrypt(any())).thenReturn("enc:secret-ciphertext");
        CreatePaymentApplicationCommand command = new CreatePaymentApplicationCommand();
        command.setAppName("开放接口应用");
        command.setIpWhitelistEnabled(0);
        command.setPayloadEncryptEnabled(1);
        command.setSignAlgorithm("HMAC_SHA256");
        command.setDemoApp(0);
        command.setStatus(1);
        org.mockito.ArgumentCaptor<PaymentApplication> captor = org.mockito.ArgumentCaptor.forClass(PaymentApplication.class);

        String plaintextSecret = service.createApplication(command).getData().getAppSecret();

        verify(applicationMapper).insert(captor.capture());
        assertThat(plaintextSecret).isNotBlank();
        assertThat(captor.getValue().getAppSecret()).isEqualTo("enc:secret-ciphertext");
        assertThat(captor.getValue().getAppSecret()).doesNotContain(plaintextSecret);
        verify(sensitiveValueService).encrypt(plaintextSecret);
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("deleteApplication should reject and audit when application has related data")
    void deleteApplication_withRelations_rejectsAndAudits() {
        PaymentApplication application = application();
        when(applicationMapper.selectById(310001L)).thenReturn(application);
        when(applicationMapper.countDeleteRelations(1L, 310001L, "app_order_center", "ORDER_CENTER")).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteApplication(310001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_APPLICATION_DELETE_HAS_RELATIONS.getMessage());

        verify(applicationMapper).countDeleteRelations(1L, 310001L, "app_order_center", "ORDER_CENTER");
        verify(applicationMapper, never()).deleteById(310001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_APPLICATION,
                PaymentOperationAuditService.RESOURCE_PAYMENT_APPLICATION,
                "app_order_center",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("deleteApplication should logical delete and audit when application has no related data")
    void deleteApplication_withoutRelations_deletesAndAudits() {
        PaymentApplication application = application();
        when(applicationMapper.selectById(310001L)).thenReturn(application);
        when(applicationMapper.countDeleteRelations(1L, 310001L, "app_order_center", "ORDER_CENTER")).thenReturn(0L);
        when(applicationMapper.deleteById(310001L)).thenReturn(1);

        service.deleteApplication(310001L);

        verify(applicationMapper).countDeleteRelations(1L, 310001L, "app_order_center", "ORDER_CENTER");
        verify(applicationMapper).deleteById(310001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_APPLICATION,
                PaymentOperationAuditService.RESOURCE_PAYMENT_APPLICATION,
                "app_order_center",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    private PaymentApplication application() {
        PaymentApplication application = new PaymentApplication();
        application.setId(310001L);
        application.setTenantId(1L);
        application.setAppId("app_order_center");
        application.setAppName("订单中心");
        return application;
    }
}
