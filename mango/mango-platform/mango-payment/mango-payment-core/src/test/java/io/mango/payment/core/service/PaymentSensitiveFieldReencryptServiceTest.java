package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.vo.PaymentSensitiveFieldReencryptResultVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentSubjectBankAccountEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentSubjectBankAccountMapper;
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

class PaymentSensitiveFieldReencryptServiceTest {

    private PaymentApplicationMapper applicationMapper;
    private PaymentEnterpriseSubjectMapper subjectMapper;
    private PaymentSubjectBankAccountMapper bankAccountMapper;
    private PaymentSensitiveValueService sensitiveValueService;
    private PaymentOperationAuditService auditService;
    private PaymentSensitiveFieldReencryptService service;

    @BeforeEach
    void setUp() {
        applicationMapper = mock(PaymentApplicationMapper.class);
        subjectMapper = mock(PaymentEnterpriseSubjectMapper.class);
        bankAccountMapper = mock(PaymentSubjectBankAccountMapper.class);
        sensitiveValueService = mock(PaymentSensitiveValueService.class);
        auditService = mock(PaymentOperationAuditService.class);
        service = new PaymentSensitiveFieldReencryptService(
                applicationMapper,
                subjectMapper,
                bankAccountMapper,
                sensitiveValueService,
                auditService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("reencryptCurrentTenant should encrypt historical plaintext fields and audit counts")
    void reencryptCurrentTenant_encryptsPlaintextFields() {
        PaymentApplication application = application("plain-app-secret");
        PaymentEnterpriseSubject subject = subject("91310000MA1PAY001X", "6222000000000001");
        PaymentSubjectBankAccountEntity bankAccount = bankAccount("6222000000000001");
        when(applicationMapper.selectList(anyApplicationWrapper())).thenReturn(List.of(application));
        when(subjectMapper.selectList(anySubjectWrapper())).thenReturn(List.of(subject));
        when(bankAccountMapper.selectList(anyBankAccountWrapper())).thenReturn(List.of(bankAccount));
        when(applicationMapper.updateById(any(PaymentApplication.class))).thenReturn(1);
        when(subjectMapper.updateById(any(PaymentEnterpriseSubject.class))).thenReturn(1);
        when(bankAccountMapper.updateById(any(PaymentSubjectBankAccountEntity.class))).thenReturn(1);
        when(sensitiveValueService.isEncrypted("plain-app-secret")).thenReturn(false);
        when(sensitiveValueService.isEncrypted("91310000MA1PAY001X")).thenReturn(false);
        when(sensitiveValueService.isEncrypted("6222000000000001")).thenReturn(false);
        when(sensitiveValueService.encrypt("plain-app-secret")).thenReturn("enc:app-secret");
        when(sensitiveValueService.encrypt("91310000MA1PAY001X")).thenReturn("enc:credit-code");
        when(sensitiveValueService.encrypt("6222000000000001")).thenReturn("enc:bank-account");
        when(sensitiveValueService.stableHash("91310000MA1PAY001X")).thenReturn("credit-hash");
        ArgumentCaptor<PaymentApplication> applicationCaptor = ArgumentCaptor.forClass(PaymentApplication.class);
        ArgumentCaptor<PaymentEnterpriseSubject> subjectCaptor = ArgumentCaptor.forClass(PaymentEnterpriseSubject.class);
        ArgumentCaptor<PaymentSubjectBankAccountEntity> accountCaptor =
                ArgumentCaptor.forClass(PaymentSubjectBankAccountEntity.class);

        PaymentSensitiveFieldReencryptResultVO result = service.reencryptCurrentTenant(100);

        assertThat(result.getApplicationSecretCount()).isEqualTo(1);
        assertThat(result.getEnterpriseCreditCodeCount()).isEqualTo(1);
        assertThat(result.getEnterpriseBankAccountCount()).isEqualTo(1);
        assertThat(result.getSubjectBankAccountCount()).isEqualTo(1);
        assertThat(result.getTotalCount()).isEqualTo(4);
        verify(applicationMapper).updateById(applicationCaptor.capture());
        verify(subjectMapper).updateById(subjectCaptor.capture());
        verify(bankAccountMapper).updateById(accountCaptor.capture());
        assertThat(applicationCaptor.getValue().getAppSecret()).isEqualTo("enc:app-secret");
        assertThat(subjectCaptor.getValue().getCreditCode()).isEqualTo("enc:credit-code");
        assertThat(subjectCaptor.getValue().getCreditCodeHash()).isEqualTo("credit-hash");
        assertThat(subjectCaptor.getValue().getBankAccountNo()).isEqualTo("enc:bank-account");
        assertThat(accountCaptor.getValue().getAccountNo()).isEqualTo("enc:bank-account");
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_REENCRYPT_SENSITIVE_FIELDS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_SENSITIVE_FIELDS,
                "tenant:1,count:4",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("reencryptCurrentTenant should skip encrypted rows and remain idempotent")
    void reencryptCurrentTenant_skipsEncryptedRows() {
        when(applicationMapper.selectList(anyApplicationWrapper())).thenReturn(List.of(application("enc:app-secret")));
        when(subjectMapper.selectList(anySubjectWrapper())).thenReturn(List.of(subject("enc:credit-code", "enc:bank-account")));
        when(bankAccountMapper.selectList(anyBankAccountWrapper())).thenReturn(List.of(bankAccount("enc:bank-account")));
        when(sensitiveValueService.isEncrypted("enc:app-secret")).thenReturn(true);
        when(sensitiveValueService.isEncrypted("enc:credit-code")).thenReturn(true);
        when(sensitiveValueService.isEncrypted("enc:bank-account")).thenReturn(true);

        PaymentSensitiveFieldReencryptResultVO result = service.reencryptCurrentTenant(100);

        assertThat(result.getTotalCount()).isZero();
        verify(applicationMapper, never()).updateById(any(PaymentApplication.class));
        verify(subjectMapper, never()).updateById(any(PaymentEnterpriseSubject.class));
        verify(bankAccountMapper, never()).updateById(any(PaymentSubjectBankAccountEntity.class));
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_REENCRYPT_SENSITIVE_FIELDS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_SENSITIVE_FIELDS,
                "tenant:1,count:0",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("reencryptCurrentTenant should reject invalid batch limit")
    void reencryptCurrentTenant_rejectsInvalidLimit() {
        assertThatThrownBy(() -> service.reencryptCurrentTenant(1001))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("重加密批量大小");
    }

    private PaymentApplication application(String appSecret) {
        PaymentApplication application = new PaymentApplication();
        application.setId(310001L);
        application.setTenantId(1L);
        application.setAppId("app_order_center");
        application.setAppSecret(appSecret);
        return application;
    }

    private PaymentEnterpriseSubject subject(String creditCode, String bankAccountNo) {
        PaymentEnterpriseSubject subject = new PaymentEnterpriseSubject();
        subject.setId(320001L);
        subject.setTenantId(1L);
        subject.setCreditCode(creditCode);
        subject.setBankAccountNo(bankAccountNo);
        return subject;
    }

    private PaymentSubjectBankAccountEntity bankAccount(String accountNo) {
        PaymentSubjectBankAccountEntity account = new PaymentSubjectBankAccountEntity();
        account.setId(321001L);
        account.setTenantId(1L);
        account.setSubjectId(320001L);
        account.setAccountNo(accountNo);
        return account;
    }

    @SuppressWarnings("unchecked")
    private Wrapper<PaymentApplication> anyApplicationWrapper() {
        return (Wrapper<PaymentApplication>) any(Wrapper.class);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<PaymentEnterpriseSubject> anySubjectWrapper() {
        return (Wrapper<PaymentEnterpriseSubject>) any(Wrapper.class);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<PaymentSubjectBankAccountEntity> anyBankAccountWrapper() {
        return (Wrapper<PaymentSubjectBankAccountEntity>) any(Wrapper.class);
    }
}
