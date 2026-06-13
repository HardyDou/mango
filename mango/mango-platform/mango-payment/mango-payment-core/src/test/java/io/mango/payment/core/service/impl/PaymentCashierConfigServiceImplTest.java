package io.mango.payment.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentCashierConfigCommand;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentCashierConfig;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.service.PaymentOperationAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentCashierConfigServiceImplTest {

    private PaymentCashierConfigMapper cashierConfigMapper;
    private PaymentApplicationMapper applicationMapper;
    private PaymentEnterpriseSubjectMapper subjectMapper;
    private PaymentMethodMapper methodMapper;
    private PaymentOperationAuditService auditService;
    private PaymentCashierConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        cashierConfigMapper = mock(PaymentCashierConfigMapper.class);
        applicationMapper = mock(PaymentApplicationMapper.class);
        subjectMapper = mock(PaymentEnterpriseSubjectMapper.class);
        methodMapper = mock(PaymentMethodMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        service = new PaymentCashierConfigServiceImpl(cashierConfigMapper, applicationMapper, subjectMapper, methodMapper, auditService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("createCashierConfig should validate real relations and audit")
    void createCashierConfig_validCommand_insertsAndAudits() {
        mockValidRelations();
        when(cashierConfigMapper.selectCount(any())).thenReturn(0L);
        ArgumentCaptor<PaymentCashierConfig> captor = ArgumentCaptor.forClass(PaymentCashierConfig.class);

        service.createCashierConfig(command());

        verify(cashierConfigMapper).insert(captor.capture());
        PaymentCashierConfig entity = captor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getApplicationId()).isEqualTo(310001L);
        assertThat(entity.getEnterpriseSubjectIds()).isEqualTo("320001");
        assertThat(entity.getMethodCodes()).isEqualTo("PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT");
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CREATE_CASHIER_CONFIG,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CASHIER_CONFIG,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("createCashierConfig should reject duplicate enabled default cashier")
    void createCashierConfig_duplicateDefault_rejects() {
        mockValidRelations();
        when(cashierConfigMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createCashierConfig(command()))
                .isInstanceOf(BizException.class)
                .hasMessage("同一应用只能启用一个默认收银台");

        verify(cashierConfigMapper, never()).insert(any(PaymentCashierConfig.class));
    }

    @Test
    @DisplayName("createCashierConfig should reject display order outside visible method codes")
    void createCashierConfig_invalidDisplayOrder_rejects() {
        mockValidRelations();
        SavePaymentCashierConfigCommand command = command();
        command.setMethodDisplayOrder("PERSONAL_ALIPAY_H5");

        assertThatThrownBy(() -> service.createCashierConfig(command))
                .isInstanceOf(BizException.class)
                .hasMessage("支付方式展示顺序必须包含在可见支付方式中");

        verify(cashierConfigMapper, never()).insert(any(PaymentCashierConfig.class));
    }

    @Test
    @DisplayName("deleteCashierConfig should reject and audit when cashier has related payment data")
    void deleteCashierConfig_withRelations_rejectsAndAudits() {
        PaymentCashierConfig config = config();
        when(cashierConfigMapper.selectById(350001L)).thenReturn(config);
        when(cashierConfigMapper.countDeleteRelations(1L, 350001L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteCashierConfig(350001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_CASHIER_CONFIG_DELETE_HAS_RELATIONS.getMessage());

        verify(cashierConfigMapper, never()).deleteById(350001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_CASHIER_CONFIG,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CASHIER_CONFIG,
                "350001",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("deleteCashierConfig should logical delete and audit when cashier is unused")
    void deleteCashierConfig_withoutRelations_deletesAndAudits() {
        PaymentCashierConfig config = config();
        when(cashierConfigMapper.selectById(350001L)).thenReturn(config);
        when(cashierConfigMapper.countDeleteRelations(1L, 350001L)).thenReturn(0L);
        when(cashierConfigMapper.deleteById(350001L)).thenReturn(1);

        service.deleteCashierConfig(350001L);

        verify(cashierConfigMapper).deleteById(350001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_CASHIER_CONFIG,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CASHIER_CONFIG,
                "350001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    private void mockValidRelations() {
        when(applicationMapper.selectById(310001L)).thenReturn(application());
        when(subjectMapper.selectById(320001L)).thenReturn(subject());
        when(methodMapper.selectOne(any())).thenReturn(method());
    }

    private SavePaymentCashierConfigCommand command() {
        SavePaymentCashierConfigCommand command = new SavePaymentCashierConfigCommand();
        command.setCashierName("订单中心 Web 收银台");
        command.setApplicationId(310001L);
        command.setDefaultCashier(1);
        command.setEnterpriseSubjectIds("320001");
        command.setMethodCodes("PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT");
        command.setDefaultMethodCode("PERSONAL_WECHAT_QR");
        command.setMethodDisplayOrder("PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT");
        command.setDisplayConfig("{\"subtitle\":\"请确认订单金额后选择支付方式\"}");
        command.setStatus(1);
        return command;
    }

    private PaymentCashierConfig config() {
        PaymentCashierConfig config = new PaymentCashierConfig();
        config.setId(350001L);
        config.setTenantId(1L);
        config.setCashierName("订单中心 Web 收银台");
        config.setApplicationId(310001L);
        return config;
    }

    private PaymentApplication application() {
        PaymentApplication application = new PaymentApplication();
        application.setId(310001L);
        application.setTenantId(1L);
        application.setAppName("订单中心");
        return application;
    }

    private PaymentEnterpriseSubject subject() {
        PaymentEnterpriseSubject subject = new PaymentEnterpriseSubject();
        subject.setId(320001L);
        subject.setTenantId(1L);
        subject.setSubjectName("芒果科技有限公司");
        return subject;
    }

    private PaymentMethod method() {
        PaymentMethod method = new PaymentMethod();
        method.setId(340001L);
        method.setTenantId(1L);
        method.setMethodCode("PERSONAL_WECHAT_QR");
        method.setMethodName("微信扫码");
        return method;
    }
}
