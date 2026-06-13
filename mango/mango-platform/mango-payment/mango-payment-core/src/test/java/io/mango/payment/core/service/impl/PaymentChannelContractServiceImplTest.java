package io.mango.payment.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.RotatePaymentChannelContractCertificateCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCapabilityCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCommand;
import io.mango.payment.api.vo.PaymentChannelCertificateExpiryVO;
import io.mango.payment.core.entity.PaymentChannelCertificateRotationRecordEntity;
import io.mango.payment.core.entity.PaymentChannel;
import io.mango.payment.core.entity.PaymentChannelCapability;
import io.mango.payment.core.entity.PaymentChannelContract;
import io.mango.payment.core.entity.PaymentChannelContractCapability;
import io.mango.payment.core.entity.PaymentChannelContractValueEntity;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.mapper.PaymentChannelCertificateRotationRecordMapper;
import io.mango.payment.core.mapper.PaymentChannelCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelContractValueMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.service.PaymentOperationAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentChannelContractServiceImplTest {

    private PaymentChannelContractMapper contractMapper;
    private PaymentChannelCapabilityMapper channelCapabilityMapper;
    private PaymentChannelContractCapabilityMapper contractCapabilityMapper;
    private PaymentChannelContractValueMapper contractValueMapper;
    private PaymentChannelCertificateRotationRecordMapper certificateRotationRecordMapper;
    private PaymentChannelContractServiceImpl service;

    @BeforeEach
    void setUp() {
        contractMapper = mock(PaymentChannelContractMapper.class);
        PaymentEnterpriseSubjectMapper subjectMapper = mock(PaymentEnterpriseSubjectMapper.class);
        PaymentChannelMapper channelMapper = mock(PaymentChannelMapper.class);
        channelCapabilityMapper = mock(PaymentChannelCapabilityMapper.class);
        contractCapabilityMapper = mock(PaymentChannelContractCapabilityMapper.class);
        contractValueMapper = mock(PaymentChannelContractValueMapper.class);
        certificateRotationRecordMapper = mock(PaymentChannelCertificateRotationRecordMapper.class);
        PaymentOperationAuditService auditService = mock(PaymentOperationAuditService.class);
        ICryptoService cryptoService = mock(ICryptoService.class);
        service = new PaymentChannelContractServiceImpl(
                contractMapper,
                subjectMapper,
                channelMapper,
                channelCapabilityMapper,
                contractCapabilityMapper,
                contractValueMapper,
                certificateRotationRecordMapper,
                auditService,
                new ObjectMapper(),
                cryptoService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        when(subjectMapper.selectById(320001L)).thenReturn(subject());
        when(channelMapper.selectById(330002L)).thenReturn(channel());
        when(channelCapabilityMapper.selectById(332001L)).thenReturn(channelCapability());
        when(contractCapabilityMapper.selectList(any())).thenReturn(List.of());
        when(cryptoService.encrypt("private-key")).thenReturn("ciphertext-value");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("deleteChannelContract should reject and audit when contract has route or order relations")
    void deleteChannelContract_withRelations_rejectsAndAudits() {
        PaymentOperationAuditService auditService = auditService();
        service = service(auditService);
        PaymentChannelContract contract = contract();
        when(contractMapper.selectById(331002L)).thenReturn(contract);
        when(contractMapper.countDeleteRelations(1L, 331002L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteChannelContract(331002L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_CHANNEL_CONTRACT_DELETE_HAS_RELATIONS.getMessage());

        verify(contractMapper, never()).deleteById(331002L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_CHANNEL_CONTRACT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_CONTRACT,
                "331002",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("deleteChannelContract should delete capability rows and audit when no relations exist")
    void deleteChannelContract_withoutRelations_deletesAndAudits() {
        PaymentOperationAuditService auditService = auditService();
        service = service(auditService);
        PaymentChannelContract contract = contract();
        when(contractMapper.selectById(331002L)).thenReturn(contract);
        when(contractMapper.countDeleteRelations(1L, 331002L)).thenReturn(0L);
        when(contractMapper.deleteById(331002L)).thenReturn(1);

        service.deleteChannelContract(331002L);

        verify(contractValueMapper).deletePhysicallyByContractId(331002L, 1L);
        verify(contractCapabilityMapper).deletePhysicallyByContractId(331002L, 1L);
        verify(contractMapper).deleteById(331002L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_CHANNEL_CONTRACT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_CONTRACT,
                "331002",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("createChannelContract should encrypt sensitive template values and detail should mask them")
    void createChannelContract_encryptsSensitiveValues() {
        org.mockito.ArgumentCaptor<PaymentChannelContract> captor = org.mockito.ArgumentCaptor.forClass(PaymentChannelContract.class);
        org.mockito.ArgumentCaptor<PaymentChannelContractValueEntity> valueCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentChannelContractValueEntity.class);
        SavePaymentChannelContractCommand command = new SavePaymentChannelContractCommand();
        command.setContractName("通联签约");
        command.setSubjectId(320001L);
        command.setChannelId(330002L);
        command.setMerchantNo("MCH001");
        command.setConfigValuesJson("{\"merchantNo\":\"MCH001\",\"privateKey\":\"private-key\"}");
        command.setStatus(1);
        when(contractMapper.insert(any(PaymentChannelContract.class))).thenAnswer(invocation -> {
            PaymentChannelContract entity = invocation.getArgument(0);
            entity.setId(331009L);
            return 1;
        });

        service.createChannelContract(command);

        verify(contractMapper).insert(captor.capture());
        verify(contractValueMapper, org.mockito.Mockito.times(2)).insert(valueCaptor.capture());
        PaymentChannelContract stored = captor.getValue();
        assertThat(stored.getContractCode()).startsWith("ALLINPAY_320001_");
        assertThat(stored.getEnvironment()).isEqualTo("PROD");
        assertThat(stored.getConfigValuesJson()).contains("\"privateKey\":\"enc:ciphertext-value\"");
        assertThat(stored.getConfigValuesJson()).doesNotContain("private-key\"");
        PaymentChannelContractValueEntity secretValue = valueCaptor.getAllValues().stream()
                .filter(row -> "privateKey".equals(row.getFieldCode()))
                .findFirst()
                .orElseThrow();
        assertThat(secretValue.getEncryptedValue()).isEqualTo("ciphertext-value");
        assertThat(secretValue.getValueText()).isNull();
        assertThat(secretValue.getSensitiveFlag()).isEqualTo(1);

        stored.setId(331009L);
        stored.setTenantId(1L);
        when(contractMapper.selectById(331009L)).thenReturn(stored);
        String masked = service.detailChannelContract(331009L).getData().getConfigValuesJson();
        assertThat(masked).contains("\"privateKey\":\"******\"");
        assertThat(masked).doesNotContain("ciphertext-value");
    }

    @Test
    @DisplayName("detailChannelContract should display non-sensitive fuiou public key and mask private key")
    void detailChannelContract_fuiouPublicKeyVisible_privateKeyMasked() {
        PaymentChannel fuiouChannel = new PaymentChannel();
        fuiouChannel.setId(330005L);
        fuiouChannel.setTenantId(1L);
        fuiouChannel.setChannelCode("FUIOU_PAY");
        fuiouChannel.setChannelName("富友支付");
        fuiouChannel.setEnvironment("PROD");
        fuiouChannel.setFieldTemplateJson("""
                [
                  {"name":"privateKey","label":"商户 RSA 私钥","component":"textarea","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":1},
                  {"name":"fuiouPublicKey","label":"富友 RSA 公钥","component":"textarea","dataType":"string","required":true,"sensitive":false,"encrypted":false,"masked":false,"sort":2}
                ]
                """);
        PaymentChannelMapper channelMapper = mock(PaymentChannelMapper.class);
        when(channelMapper.selectById(330005L)).thenReturn(fuiouChannel);
        service = service(auditService(), channelCapabilityMapper, channelMapper);
        PaymentChannelContract contract = contract();
        contract.setChannelId(330005L);
        contract.setConfigValuesJson("""
                {
                  "privateKey": "enc:private-ciphertext",
                  "fuiouPublicKey": "fuiou-public-key"
                }
                """);
        when(contractMapper.selectById(331002L)).thenReturn(contract);

        String configValuesJson = service.detailChannelContract(331002L).getData().getConfigValuesJson();

        assertThat(configValuesJson).contains("\"privateKey\":\"******\"");
        assertThat(configValuesJson).contains("\"fuiouPublicKey\":\"fuiou-public-key\"");
        assertThat(configValuesJson).doesNotContain("private-ciphertext");
    }

    @Test
    @DisplayName("createChannelContract should store file template values as file id only")
    void createChannelContract_storesFileIdValues() {
        org.mockito.ArgumentCaptor<PaymentChannelContractValueEntity> valueCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentChannelContractValueEntity.class);
        PaymentChannel channel = channelWithFileTemplate();
        when(channelCapabilityMapper.selectById(332001L)).thenReturn(channelCapability());
        when(contractMapper.insert(any(PaymentChannelContract.class))).thenAnswer(invocation -> {
            PaymentChannelContract entity = invocation.getArgument(0);
            entity.setId(331010L);
            return 1;
        });
        SavePaymentChannelContractCommand command = contractCommand();
        command.setConfigValuesJson("{\"merchantNo\":\"MCH001\",\"certificateFileId\":\"900001\"}");
        PaymentChannelMapper channelMapper = mock(PaymentChannelMapper.class);
        when(channelMapper.selectById(330002L)).thenReturn(channel);
        service = service(auditService(), channelCapabilityMapper, channelMapper);

        service.createChannelContract(command);

        verify(contractValueMapper, org.mockito.Mockito.times(2)).insert(valueCaptor.capture());
        PaymentChannelContractValueEntity fileValue = valueCaptor.getAllValues().stream()
                .filter(row -> "certificateFileId".equals(row.getFieldCode()))
                .findFirst()
                .orElseThrow();
        assertThat(fileValue.getFileId()).isEqualTo(900001L);
        assertThat(fileValue.getValueText()).isNull();
        assertThat(fileValue.getEncryptedValue()).isNull();
        assertThat(fileValue.getValueSource()).isEqualTo("FILE");
    }

    @Test
    @DisplayName("createChannelContract should default offline collection account from enterprise subject")
    void createChannelContract_offlineCollection_defaultsAccountFromSubject() {
        org.mockito.ArgumentCaptor<PaymentChannelContract> contractCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentChannelContract.class);
        org.mockito.ArgumentCaptor<PaymentChannelContractValueEntity> valueCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentChannelContractValueEntity.class);
        PaymentChannelMapper channelMapper = mock(PaymentChannelMapper.class);
        when(channelMapper.selectById(330004L)).thenReturn(offlineCollectionChannel());
        when(contractMapper.insert(any(PaymentChannelContract.class))).thenAnswer(invocation -> {
            PaymentChannelContract entity = invocation.getArgument(0);
            entity.setId(331004L);
            return 1;
        });
        service = service(auditService(), channelCapabilityMapper, channelMapper);
        SavePaymentChannelContractCommand command = new SavePaymentChannelContractCommand();
        command.setContractName("芒果科技线下收款签约");
        command.setSubjectId(320001L);
        command.setChannelId(330004L);
        command.setMerchantNo("OFFLINE_COLLECTION_MERCHANT_001");
        command.setConfigValuesJson("{}");
        command.setStatus(1);

        service.createChannelContract(command);

        verify(contractMapper).insert(contractCaptor.capture());
        verify(contractValueMapper, org.mockito.Mockito.times(3)).insert(valueCaptor.capture());
        PaymentChannelContract stored = contractCaptor.getValue();
        assertThat(stored.getConfigValuesJson()).contains("\"accountName\":\"芒果科技有限公司\"");
        assertThat(stored.getConfigValuesJson()).contains("\"accountNo\":\"enc:subject-account\"");
        assertThat(stored.getConfigValuesJson()).contains("\"bankName\":\"招商银行上海分行\"");
        PaymentChannelContractValueEntity accountValue = valueCaptor.getAllValues().stream()
                .filter(row -> "accountNo".equals(row.getFieldCode()))
                .findFirst()
                .orElseThrow();
        assertThat(accountValue.getEncryptedValue()).isEqualTo("subject-account");
        assertThat(accountValue.getValueText()).isNull();
        assertThat(accountValue.getSensitiveFlag()).isEqualTo(1);
    }

    @Test
    @DisplayName("createChannelContract should reject file access url in file template value")
    void createChannelContract_rejectsFileAccessUrl() {
        PaymentChannel channel = channelWithFileTemplate();
        PaymentChannelMapper channelMapper = mock(PaymentChannelMapper.class);
        when(channelMapper.selectById(330002L)).thenReturn(channel);
        when(channelCapabilityMapper.selectById(332001L)).thenReturn(channelCapability());
        service = service(auditService(), channelCapabilityMapper, channelMapper);
        SavePaymentChannelContractCommand command = contractCommand();
        command.setConfigValuesJson("{\"merchantNo\":\"MCH001\",\"certificateFileId\":\"https://files.example/cert.pem\"}");

        assertThatThrownBy(() -> service.createChannelContract(command))
                .isInstanceOf(BizException.class)
                .hasMessage("证书文件只能保存文件 ID");

        verify(contractMapper, never()).insert(any(PaymentChannelContract.class));
    }

    @Test
    @DisplayName("updateChannelContract should update existing capability instead of recreating route target")
    void updateChannelContract_updatesExistingCapability() {
        PaymentOperationAuditService auditService = auditService();
        PaymentChannelCapabilityMapper channelCapabilityMapper = mock(PaymentChannelCapabilityMapper.class);
        when(channelCapabilityMapper.selectById(332001L)).thenReturn(channelCapability());
        service = service(auditService, channelCapabilityMapper);
        PaymentChannelContract contract = contract();
        PaymentChannelContractCapability existing = contractCapability();
        when(contractMapper.selectById(331002L)).thenReturn(contract);
        when(contractCapabilityMapper.selectList(any())).thenReturn(List.of(existing));
        when(contractMapper.updateById(any(PaymentChannelContract.class))).thenReturn(1);

        SavePaymentChannelContractCommand command = contractCommand();
        command.setId(331002L);
        command.setCapabilities(List.of(capabilityCommand()));

        service.updateChannelContract(command);

        org.mockito.ArgumentCaptor<PaymentChannelContract> contractCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentChannelContract.class);
        verify(contractMapper).updateById(contractCaptor.capture());
        assertThat(contractCaptor.getValue().getContractCode()).isEqualTo("ALLINPAY_MANGO_TECH");
        assertThat(contractCaptor.getValue().getEnvironment()).isEqualTo("PROD");
        verify(contractCapabilityMapper).updateById(any(PaymentChannelContractCapability.class));
        verify(contractCapabilityMapper, never()).deletePhysicallyById(331102L, 1L);
        verify(contractCapabilityMapper, never()).insert(any(PaymentChannelContractCapability.class));
    }

    @Test
    @DisplayName("updateChannelContract should reject removing capability when route references it")
    void updateChannelContract_rejectsRemovingRoutedCapability() {
        PaymentOperationAuditService auditService = auditService();
        service = service(auditService, mock(PaymentChannelCapabilityMapper.class));
        PaymentChannelContract contract = contract();
        when(contractMapper.selectById(331002L)).thenReturn(contract);
        when(contractCapabilityMapper.selectList(any())).thenReturn(List.of(contractCapability()));
        when(contractCapabilityMapper.countRouteRelations(1L, 331102L)).thenReturn(1L);

        SavePaymentChannelContractCommand command = contractCommand();
        command.setId(331002L);
        command.setCapabilities(List.of());

        assertThatThrownBy(() -> service.updateChannelContract(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("签约能力已被路由引用");

        verify(contractCapabilityMapper, never()).deletePhysicallyById(331102L, 1L);
    }

    @Test
    @DisplayName("createChannelContract should reject negative capability amount")
    void createChannelContract_rejectsNegativeCapabilityAmount() {
        SavePaymentChannelContractCommand command = contractCommand();
        SavePaymentChannelContractCapabilityCommand capability = capabilityCommand();
        capability.setMinAmount(-1L);
        command.setCapabilities(List.of(capability));

        assertThatThrownBy(() -> service.createChannelContract(command))
                .isInstanceOf(BizException.class)
                .hasMessage("签约能力最小金额不能小于 0 分");

        verify(contractMapper, never()).insert(any(PaymentChannelContract.class));
    }

    @Test
    @DisplayName("listExpiringCertificates should query current tenant within warning days")
    void listExpiringCertificates_queriesCurrentTenant() {
        PaymentChannelCertificateExpiryVO row = new PaymentChannelCertificateExpiryVO();
        row.setContractId(331002L);
        row.setContractCapabilityId(331102L);
        row.setCertificateExpireTime(LocalDateTime.now().plusDays(5));
        when(contractCapabilityMapper.selectExpiringCertificates(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(List.of(row));

        List<PaymentChannelCertificateExpiryVO> rows = service.listExpiringCertificates(7).getData();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getContractCapabilityId()).isEqualTo(331102L);
        verify(contractCapabilityMapper).selectExpiringCertificates(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    @DisplayName("listExpiringCertificates should reject warning days out of range")
    void listExpiringCertificates_rejectsOutOfRangeDays() {
        assertThatThrownBy(() -> service.listExpiringCertificates(366))
                .isInstanceOf(BizException.class)
                .hasMessage("证书提醒天数必须在 0 到 365 之间");
    }

    @Test
    @DisplayName("rotateCertificate should update file id, certificate expiry, rotation record and audit")
    void rotateCertificate_updatesFileAndRecordsAudit() {
        PaymentOperationAuditService auditService = auditService();
        PaymentChannelMapper channelMapper = mock(PaymentChannelMapper.class);
        when(channelMapper.selectById(330002L)).thenReturn(channelWithFileTemplate());
        when(contractMapper.selectById(331002L)).thenReturn(contractWithCertificateFile());
        when(contractCapabilityMapper.selectById(331102L)).thenReturn(contractCapabilityWithCertificateExpireTime());
        PaymentChannelContractValueEntity currentValue = new PaymentChannelContractValueEntity();
        currentValue.setId(991001L);
        currentValue.setContractId(331002L);
        currentValue.setTenantId(1L);
        currentValue.setFieldCode("certificateFileId");
        currentValue.setFileId(900001L);
        currentValue.setValueSource("FILE");
        when(contractValueMapper.selectOne(any())).thenReturn(currentValue);
        service = service(auditService, channelCapabilityMapper, channelMapper);
        LocalDateTime newExpireTime = LocalDateTime.now().plusDays(90);
        RotatePaymentChannelContractCertificateCommand command = new RotatePaymentChannelContractCertificateCommand();
        command.setContractId(331002L);
        command.setContractCapabilityId(331102L);
        command.setCertificateFieldCode("certificateFileId");
        command.setNewCertificateFileId(900002L);
        command.setNewCertificateExpireTime(newExpireTime);
        command.setRotateReason("证书到期轮换");
        org.mockito.ArgumentCaptor<PaymentChannelContract> contractCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentChannelContract.class);
        org.mockito.ArgumentCaptor<PaymentChannelContractCapability> capabilityCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentChannelContractCapability.class);
        org.mockito.ArgumentCaptor<PaymentChannelContractValueEntity> valueCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentChannelContractValueEntity.class);
        org.mockito.ArgumentCaptor<PaymentChannelCertificateRotationRecordEntity> recordCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentChannelCertificateRotationRecordEntity.class);

        service.rotateCertificate(command);

        verify(contractMapper).updateById(contractCaptor.capture());
        assertThat(contractCaptor.getValue().getConfigValuesJson()).contains("\"certificateFileId\":\"900002\"");
        verify(contractCapabilityMapper).updateById(capabilityCaptor.capture());
        assertThat(capabilityCaptor.getValue().getCertificateExpireTime()).isEqualTo(newExpireTime);
        verify(contractValueMapper).updateById(valueCaptor.capture());
        assertThat(valueCaptor.getValue().getFileId()).isEqualTo(900002L);
        assertThat(valueCaptor.getValue().getValueText()).isNull();
        assertThat(valueCaptor.getValue().getEncryptedValue()).isNull();
        verify(certificateRotationRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getOldCertificateFileId()).isEqualTo(900001L);
        assertThat(recordCaptor.getValue().getNewCertificateFileId()).isEqualTo(900002L);
        assertThat(recordCaptor.getValue().getOldCertificateExpireTime()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
        assertThat(recordCaptor.getValue().getNewCertificateExpireTime()).isEqualTo(newExpireTime);
        assertThat(recordCaptor.getValue().getRotateReason()).isEqualTo("证书到期轮换");
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_ROTATE_CHANNEL_CERTIFICATE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_CONTRACT,
                "331002",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("rotateCertificate should reject unknown certificate file field")
    void rotateCertificate_rejectsUnknownFileField() {
        PaymentChannelMapper channelMapper = mock(PaymentChannelMapper.class);
        when(channelMapper.selectById(330002L)).thenReturn(channelWithFileTemplate());
        when(contractMapper.selectById(331002L)).thenReturn(contractWithCertificateFile());
        when(contractCapabilityMapper.selectById(331102L)).thenReturn(contractCapabilityWithCertificateExpireTime());
        service = service(auditService(), channelCapabilityMapper, channelMapper);
        RotatePaymentChannelContractCertificateCommand command = new RotatePaymentChannelContractCertificateCommand();
        command.setContractId(331002L);
        command.setContractCapabilityId(331102L);
        command.setCertificateFieldCode("privateKey");
        command.setNewCertificateFileId(900002L);
        command.setNewCertificateExpireTime(LocalDateTime.now().plusDays(90));
        command.setRotateReason("证书到期轮换");

        assertThatThrownBy(() -> service.rotateCertificate(command))
                .isInstanceOf(BizException.class)
                .hasMessage("证书字段必须是通道模板中的文件 ID 字段");

        verify(certificateRotationRecordMapper, never()).insert(any(PaymentChannelCertificateRotationRecordEntity.class));
        verify(contractMapper, never()).updateById(any(PaymentChannelContract.class));
    }

    private PaymentChannelContractServiceImpl service(PaymentOperationAuditService auditService) {
        PaymentChannelCapabilityMapper channelCapabilityMapper = mock(PaymentChannelCapabilityMapper.class);
        when(channelCapabilityMapper.selectById(332001L)).thenReturn(channelCapability());
        return service(auditService, channelCapabilityMapper);
    }

    private PaymentChannelContractServiceImpl service(
            PaymentOperationAuditService auditService,
            PaymentChannelCapabilityMapper channelCapabilityMapper) {
        PaymentChannelMapper channelMapper = mock(PaymentChannelMapper.class);
        when(channelMapper.selectById(330002L)).thenReturn(channel());
        return service(auditService, channelCapabilityMapper, channelMapper);
    }

    private PaymentChannelContractServiceImpl service(
            PaymentOperationAuditService auditService,
            PaymentChannelCapabilityMapper channelCapabilityMapper,
            PaymentChannelMapper channelMapper) {
        PaymentEnterpriseSubjectMapper subjectMapper = mock(PaymentEnterpriseSubjectMapper.class);
        ICryptoService cryptoService = mock(ICryptoService.class);
        when(subjectMapper.selectById(320001L)).thenReturn(subject());
        when(contractCapabilityMapper.selectList(any())).thenReturn(List.of());
        when(cryptoService.encrypt("private-key")).thenReturn("ciphertext-value");
        return new PaymentChannelContractServiceImpl(
                contractMapper,
                subjectMapper,
                channelMapper,
                channelCapabilityMapper,
                contractCapabilityMapper,
                contractValueMapper,
                certificateRotationRecordMapper,
                auditService,
                new ObjectMapper(),
                cryptoService);
    }

    private PaymentOperationAuditService auditService() {
        return mock(PaymentOperationAuditService.class);
    }

    private PaymentChannelContract contract() {
        PaymentChannelContract contract = new PaymentChannelContract();
        contract.setId(331002L);
        contract.setTenantId(1L);
        contract.setContractCode("ALLINPAY_MANGO_TECH");
        contract.setContractName("芒果科技通联签约");
        contract.setSubjectId(320001L);
        contract.setChannelId(330002L);
        contract.setEnvironment("PROD");
        contract.setMerchantNo("ALLINPAY_MERCHANT_001");
        contract.setStatus(1);
        return contract;
    }

    private PaymentChannelContract contractWithCertificateFile() {
        PaymentChannelContract contract = contract();
        contract.setConfigValuesJson("{\"merchantNo\":\"ALLINPAY_MERCHANT_001\",\"certificateFileId\":\"900001\"}");
        return contract;
    }

    private SavePaymentChannelContractCommand contractCommand() {
        SavePaymentChannelContractCommand command = new SavePaymentChannelContractCommand();
        command.setContractName("芒果科技通联签约");
        command.setSubjectId(320001L);
        command.setChannelId(330002L);
        command.setMerchantNo("ALLINPAY_MERCHANT_001");
        command.setConfigValuesJson("{\"merchantNo\":\"ALLINPAY_MERCHANT_001\",\"privateKey\":\"private-key\"}");
        command.setStatus(1);
        return command;
    }

    private SavePaymentChannelContractCapabilityCommand capabilityCommand() {
        SavePaymentChannelContractCapabilityCommand command = new SavePaymentChannelContractCapabilityCommand();
        command.setChannelCapabilityId(332001L);
        command.setFeeRate(new BigDecimal("0.0060000000"));
        command.setMinAmount(10L);
        command.setMaxAmount(880000L);
        command.setPriority(10);
        command.setStatus(1);
        return command;
    }

    private PaymentChannelCapability channelCapability() {
        PaymentChannelCapability capability = new PaymentChannelCapability();
        capability.setId(332001L);
        capability.setTenantId(1L);
        capability.setChannelId(330002L);
        capability.setMethodCode("PERSONAL_WECHAT_QR");
        capability.setTerminalType("WEB");
        capability.setMinAmount(1L);
        capability.setMaxAmount(999999L);
        capability.setStatus(1);
        return capability;
    }

    private PaymentChannelContractCapability contractCapability() {
        PaymentChannelContractCapability capability = new PaymentChannelContractCapability();
        capability.setId(331102L);
        capability.setTenantId(1L);
        capability.setContractId(331002L);
        capability.setChannelCapabilityId(332001L);
        capability.setMethodCode("PERSONAL_WECHAT_QR");
        capability.setTerminalType("WEB");
        capability.setStatus(1);
        return capability;
    }

    private PaymentChannelContractCapability contractCapabilityWithCertificateExpireTime() {
        PaymentChannelContractCapability capability = contractCapability();
        capability.setCertificateExpireTime(LocalDateTime.of(2026, 7, 1, 0, 0));
        return capability;
    }

    private PaymentEnterpriseSubject subject() {
        PaymentEnterpriseSubject subject = new PaymentEnterpriseSubject();
        subject.setId(320001L);
        subject.setTenantId(1L);
        subject.setSubjectName("芒果科技有限公司");
        subject.setBankAccountNo("enc:subject-account");
        subject.setBankName("招商银行上海分行");
        return subject;
    }

    private PaymentChannel channel() {
        PaymentChannel channel = new PaymentChannel();
        channel.setId(330002L);
        channel.setTenantId(1L);
        channel.setChannelCode("ALLINPAY");
        channel.setChannelName("通联支付通道");
        channel.setEnvironment("PROD");
        channel.setFieldTemplateJson("""
                [
                  {"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true,"sort":1},
                  {"name":"privateKey","label":"商户私钥","component":"textarea","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":2}
                ]
                """);
        return channel;
    }

    private PaymentChannel channelWithFileTemplate() {
        PaymentChannel channel = new PaymentChannel();
        channel.setId(330002L);
        channel.setTenantId(1L);
        channel.setChannelCode("ALLINPAY");
        channel.setChannelName("通联支付通道");
        channel.setEnvironment("PROD");
        channel.setFieldTemplateJson("""
                [
                  {"name":"merchantNo","label":"商户号","component":"input","dataType":"string","required":true,"sort":1},
                  {"name":"certificateFileId","label":"证书文件","component":"fileId","dataType":"fileId","required":true,"sort":2}
                ]
                """);
        return channel;
    }

    private PaymentChannel offlineCollectionChannel() {
        PaymentChannel channel = new PaymentChannel();
        channel.setId(330004L);
        channel.setTenantId(1L);
        channel.setChannelCode("OFFLINE_COLLECTION");
        channel.setChannelName("线下收款");
        channel.setEnvironment("OFFLINE_COLLECTION");
        channel.setFieldTemplateJson("""
                [
                  {"name":"accountName","label":"收款户名","component":"input","dataType":"string","required":true,"sort":1},
                  {"name":"accountNo","label":"收款账号","component":"input","dataType":"string","required":true,"sensitive":true,"encrypted":true,"masked":true,"sort":2},
                  {"name":"bankName","label":"开户行","component":"input","dataType":"string","required":true,"sort":3}
                ]
                """);
        return channel;
    }
}
