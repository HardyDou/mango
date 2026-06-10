package io.mango.payment.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentEnterpriseSubjectCommand;
import io.mango.payment.api.vo.PaymentEnterpriseSubjectVO;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentSubjectBankAccountEntity;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentSubjectBankAccountMapper;
import io.mango.payment.core.service.PaymentOperationAuditService;
import io.mango.payment.core.service.PaymentSensitiveValueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentEnterpriseSubjectServiceImplTest {

    private PaymentEnterpriseSubjectMapper subjectMapper;
    private PaymentSubjectBankAccountMapper bankAccountMapper;
    private PaymentOperationAuditService auditService;
    private PaymentSensitiveValueService sensitiveValueService;
    private PaymentEnterpriseSubjectServiceImpl service;

    @BeforeEach
    void setUp() {
        subjectMapper = mock(PaymentEnterpriseSubjectMapper.class);
        bankAccountMapper = mock(PaymentSubjectBankAccountMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        sensitiveValueService = mock(PaymentSensitiveValueService.class);
        service = new PaymentEnterpriseSubjectServiceImpl(subjectMapper, bankAccountMapper, auditService, sensitiveValueService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @Test
    @DisplayName("createEnterpriseSubject should encrypt credit code and bank account")
    void createEnterpriseSubject_encryptsSensitiveValues() {
        when(sensitiveValueService.encrypt("91310000MA1PAY001X")).thenReturn("enc:credit-ciphertext");
        when(sensitiveValueService.encrypt("6222000000000001")).thenReturn("enc:account-ciphertext");
        when(sensitiveValueService.stableHash("91310000MA1PAY001X")).thenReturn("credit-hash");
        SavePaymentEnterpriseSubjectCommand command = command();
        org.mockito.ArgumentCaptor<PaymentEnterpriseSubject> captor = org.mockito.ArgumentCaptor.forClass(PaymentEnterpriseSubject.class);
        org.mockito.ArgumentCaptor<PaymentSubjectBankAccountEntity> accountCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentSubjectBankAccountEntity.class);

        service.createEnterpriseSubject(command);

        verify(subjectMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreditCodeHash()).isEqualTo("credit-hash");
        assertThat(captor.getValue().getCreditCode()).isEqualTo("enc:credit-ciphertext");
        assertThat(captor.getValue().getBankAccountNo()).isEqualTo("enc:account-ciphertext");
        verify(bankAccountMapper).insert(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getAccountNo()).isEqualTo("enc:account-ciphertext");
        assertThat(accountCaptor.getValue().getAccountName()).isEqualTo("芒果科技有限公司");
        assertThat(accountCaptor.getValue().getBankName()).isEqualTo("招商银行");
        assertThat(accountCaptor.getValue().getAccountType()).isEqualTo("CORPORATE");
        assertThat(accountCaptor.getValue().getDefaultAccount()).isEqualTo(1);
        assertThat(accountCaptor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("detailEnterpriseSubject should expose only masked sensitive values")
    void detailEnterpriseSubject_masksSensitiveValues() {
        PaymentEnterpriseSubject subject = subject();
        subject.setCreditCode("enc:credit-ciphertext");
        subject.setBankAccountNo("enc:account-ciphertext");
        when(subjectMapper.selectById(320001L)).thenReturn(subject);
        when(sensitiveValueService.mask("enc:credit-ciphertext", 4, 4)).thenReturn("9131****001X");
        when(sensitiveValueService.mask("enc:account-ciphertext", 4, 4)).thenReturn("6222****0001");

        PaymentEnterpriseSubjectVO result = service.detailEnterpriseSubject(320001L).getData();

        assertThat(result.getCreditCodeMask()).isEqualTo("9131****001X");
        assertThat(result.getBankAccountNoMask()).isEqualTo("6222****0001");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("deleteEnterpriseSubject should reject and audit when subject has related data")
    void deleteEnterpriseSubject_withRelations_rejectsAndAudits() {
        PaymentEnterpriseSubject subject = subject();
        when(subjectMapper.selectById(320001L)).thenReturn(subject);
        when(subjectMapper.countDeleteRelations(1L, 320001L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteEnterpriseSubject(320001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_DELETE_HAS_RELATIONS.getMessage());

        verify(subjectMapper, never()).deleteById(320001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_ENTERPRISE_SUBJECT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ENTERPRISE_SUBJECT,
                "320001",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("deleteEnterpriseSubject should logical delete and audit when subject has no related data")
    void deleteEnterpriseSubject_withoutRelations_deletesAndAudits() {
        PaymentEnterpriseSubject subject = subject();
        when(subjectMapper.selectById(320001L)).thenReturn(subject);
        when(subjectMapper.countDeleteRelations(1L, 320001L)).thenReturn(0L);
        when(subjectMapper.deleteById(320001L)).thenReturn(1);

        service.deleteEnterpriseSubject(320001L);

        verify(subjectMapper).deleteById(320001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_ENTERPRISE_SUBJECT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ENTERPRISE_SUBJECT,
                "320001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("updateEnterpriseSubject should sync encrypted default bank account")
    void updateEnterpriseSubject_syncsEncryptedDefaultBankAccount() {
        PaymentEnterpriseSubject subject = subject();
        PaymentSubjectBankAccountEntity account = new PaymentSubjectBankAccountEntity();
        account.setId(330001L);
        account.setSubjectId(320001L);
        account.setTenantId(1L);
        account.setAccountNo("enc:old-account-ciphertext");
        account.setDefaultAccount(1);
        account.setDelFlag(0);
        when(subjectMapper.selectById(320001L)).thenReturn(subject);
        when(bankAccountMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(account);
        when(sensitiveValueService.encrypt("91310000MA1PAY001X")).thenReturn("enc:credit-ciphertext");
        when(sensitiveValueService.encrypt("6222000000000001")).thenReturn("enc:account-ciphertext");
        when(sensitiveValueService.stableHash("91310000MA1PAY001X")).thenReturn("credit-hash");
        SavePaymentEnterpriseSubjectCommand command = command();
        command.setId(320001L);
        org.mockito.ArgumentCaptor<PaymentSubjectBankAccountEntity> accountCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentSubjectBankAccountEntity.class);

        service.updateEnterpriseSubject(command);

        verify(bankAccountMapper).updateById(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getId()).isEqualTo(330001L);
        assertThat(accountCaptor.getValue().getAccountNo()).isEqualTo("enc:account-ciphertext");
        assertThat(accountCaptor.getValue().getAccountNo()).doesNotContain("6222000000000001");
        assertThat(accountCaptor.getValue().getBankName()).isEqualTo("招商银行");
        verify(bankAccountMapper, never()).insert(org.mockito.ArgumentMatchers.<PaymentSubjectBankAccountEntity>any());
    }

    private PaymentEnterpriseSubject subject() {
        PaymentEnterpriseSubject subject = new PaymentEnterpriseSubject();
        subject.setId(320001L);
        subject.setTenantId(1L);
        subject.setSubjectName("芒果科技有限公司");
        subject.setCreditCode("91310000MA1PAY001X");
        return subject;
    }

    private SavePaymentEnterpriseSubjectCommand command() {
        SavePaymentEnterpriseSubjectCommand command = new SavePaymentEnterpriseSubjectCommand();
        command.setSubjectName("芒果科技有限公司");
        command.setCreditCode("91310000MA1PAY001X");
        command.setBankAccountNo("6222000000000001");
        command.setBankName("招商银行");
        command.setStatus(1);
        return command;
    }
}
