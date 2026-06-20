package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.FetchPaymentChannelBillCommand;
import io.mango.payment.api.command.GenerateMangoPayVirtualBillCommand;
import io.mango.payment.api.command.ImportPaymentReconciliationCommand;
import io.mango.payment.api.vo.PaymentReconciliationVO;
import io.mango.payment.core.entity.PaymentChannelBillFetchBatchEntity;
import io.mango.payment.core.entity.PaymentChannelBillSourceEntity;
import io.mango.payment.core.entity.PaymentChannelBillDetailEntity;
import io.mango.payment.core.entity.PaymentChannel;
import io.mango.payment.core.entity.PaymentDifferenceEntity;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentChannelContract;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.entity.PaymentReconciliationEntity;
import io.mango.payment.core.entity.PaymentMangoPayScenarioControl;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentChannelBillBatchMapper;
import io.mango.payment.core.mapper.PaymentChannelBillDetailMapper;
import io.mango.payment.core.mapper.PaymentChannelBillFetchBatchMapper;
import io.mango.payment.core.mapper.PaymentChannelBillSourceMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.mapper.PaymentDifferenceMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentReconciliationMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import io.mango.payment.core.model.PaymentChannelBillItemRow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentReconciliationServiceTest {

    private PaymentReconciliationMapper reconciliationMapper;
    private PaymentChannelContractMapper channelContractMapper;
    private PaymentChannelMapper channelMapper;
    private PaymentChannelBillBatchMapper billBatchMapper;
    private PaymentChannelBillSourceMapper billSourceMapper;
    private PaymentChannelBillFetchBatchMapper billFetchBatchMapper;
    private PaymentChannelBillDetailMapper billDetailMapper;
    private PaymentDifferenceMapper differenceMapper;
    private PaymentBusinessOrderMapper businessOrderMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private PaymentRefundOrderMapper refundOrderMapper;
    private PaymentTransactionFlowMapper transactionFlowMapper;
    private PaymentOperationAuditService auditService;
    private PaymentMangoPayScenarioControlService scenarioControlService;
    private PaymentOrderStatusFlowService statusFlowService;
    private PaymentDuplicateRefundCompletionService duplicateRefundCompletionService;
    private PaymentObservabilityService observabilityService;
    private PaymentNumberService numberService;
    private PaymentReconciliationService service;

    @BeforeEach
    void setUp() {
        reconciliationMapper = mock(PaymentReconciliationMapper.class);
        channelContractMapper = mock(PaymentChannelContractMapper.class);
        channelMapper = mock(PaymentChannelMapper.class);
        billBatchMapper = mock(PaymentChannelBillBatchMapper.class);
        billSourceMapper = mock(PaymentChannelBillSourceMapper.class);
        billFetchBatchMapper = mock(PaymentChannelBillFetchBatchMapper.class);
        billDetailMapper = mock(PaymentChannelBillDetailMapper.class);
        differenceMapper = mock(PaymentDifferenceMapper.class);
        businessOrderMapper = mock(PaymentBusinessOrderMapper.class);
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        refundOrderMapper = mock(PaymentRefundOrderMapper.class);
        transactionFlowMapper = mock(PaymentTransactionFlowMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        scenarioControlService = mock(PaymentMangoPayScenarioControlService.class);
        statusFlowService = mock(PaymentOrderStatusFlowService.class);
        duplicateRefundCompletionService = mock(PaymentDuplicateRefundCompletionService.class);
        observabilityService = mock(PaymentObservabilityService.class);
        numberService = mock(PaymentNumberService.class);
        when(numberService.next(anyString())).thenAnswer(invocation -> switch (invocation.getArgument(0, String.class)) {
            case PaymentNumberService.PAY_RECON_BATCH_NO -> "RC2026060600000001";
            case PaymentNumberService.PAY_DIFF_NO -> "DF2026060600000001";
            case PaymentNumberService.PAY_FLOW_NO -> "PF2026060600000001";
            case PaymentNumberService.PAY_REFUND_FLOW_NO -> "RF2026060600000001";
            case PaymentNumberService.PAY_FEE_FLOW_NO -> "FF2026060600000001";
            default -> "NO2026060600000001";
        });
        service = new PaymentReconciliationService(
                reconciliationMapper,
                billBatchMapper,
                billSourceMapper,
                billFetchBatchMapper,
                billDetailMapper,
                channelContractMapper,
                channelMapper,
                differenceMapper,
                businessOrderMapper,
                paymentOrderMapper,
                refundOrderMapper,
                transactionFlowMapper,
                auditService,
                new PaymentChannelAdapterRegistry(List.of(new PaymentMangoPayChannelAdapter(
                        channelContractMapper,
                        reconciliationMapper,
                        scenarioControlService,
                        new PaymentMangoPayResultMappingService()))),
                new PaymentOrderStateService(),
                statusFlowService,
                duplicateRefundCompletionService,
                observabilityService,
                numberService,
                new PaymentChannelBillFileClient(List.of()),
                new ObjectMapper(),
                new TestTransactionManager());
        when(paymentOrderMapper.selectSuccessfulChannelOrdersMissingInBill(any(), anyString(), any(), any(), any()))
                .thenReturn(List.of());
        when(refundOrderMapper.selectSuccessfulChannelRefundsMissingInBill(any(), anyString(), any(), any(), any()))
                .thenReturn(List.of());
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        PaymentChannelContract contract = new PaymentChannelContract();
        contract.setId(331001L);
        contract.setTenantId(1L);
        contract.setChannelId(330001L);
        contract.setStatus(1);
        contract.setDelFlag(0);
        when(channelContractMapper.selectById(331001L)).thenReturn(contract);
        PaymentChannel channel = new PaymentChannel();
        channel.setId(330001L);
        channel.setTenantId(1L);
        channel.setChannelCode("MANGO_PAY");
        channel.setStatus(1);
        channel.setDelFlag(0);
        channel.setBillFetchModes("MANUAL,FTP,FTPS,HTTP");
        when(channelMapper.selectById(330001L)).thenReturn(channel);
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("listBillFetchModes should expose enum backed bill fetch mode options")
    void listBillFetchModes_returnsEnumBackedOptions() {
        assertThat(service.listBillFetchModes())
                .extracting(option -> option.getFetchMode() + ":" + option.getFetchModeName())
                .containsExactly("MANUAL:手动上传", "FTP:FTP 拉取", "FTPS:FTPS 拉取", "HTTP:HTTP 接口");
    }

    @Test
    @DisplayName("saveBillSource should reject fetch mode unsupported by channel definition")
    void saveBillSource_unsupportedChannelFetchMode_rejects() {
        PaymentChannel channel = new PaymentChannel();
        channel.setId(330001L);
        channel.setTenantId(1L);
        channel.setChannelCode("MANGO_PAY");
        channel.setStatus(1);
        channel.setDelFlag(0);
        channel.setBillFetchModes("MANUAL,HTTP");
        when(channelMapper.selectById(330001L)).thenReturn(channel);

        io.mango.payment.api.command.SavePaymentChannelBillSourceCommand command =
                new io.mango.payment.api.command.SavePaymentChannelBillSourceCommand();
        command.setContractId(331001L);
        command.setFetchMode("FTP");
        command.setEndpoint("ftp://127.0.0.1");
        command.setRemotePath("/bills");
        command.setEnabled(1);

        assertThatThrownBy(() -> service.saveBillSource(command))
                .isInstanceOf(BizException.class)
                .hasMessage("支付通道未声明支持该账单获取方式");
    }

    @Test
    @DisplayName("fetchChannelBill should fetch FTP bill through real protocol and import reconciliation")
    void fetchChannelBill_ftpSource_fetchesRemoteBillAndImports() throws Exception {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        String billBody = """
                {"items":[{"channelTradeNo":"FTP-PO001","tradeType":"PAYMENT","amount":9900,"fee":12,"tradeTime":"2026-06-06T10:00:00"}]}
                """;
        try (SimpleFtpServer ftpServer = new SimpleFtpServer("/bills/2026-06-06.json", billBody)) {
            PaymentChannelBillSourceEntity source = new PaymentChannelBillSourceEntity();
            source.setId(900001L);
            source.setTenantId(1L);
            source.setDelFlag(0);
            source.setEnabled(1);
            source.setChannelCode("MANGO_PAY");
            source.setFetchMode("FTP");
            source.setEndpoint("127.0.0.1:" + ftpServer.port());
            source.setRemotePath("/bills/2026-06-06.json");
            when(billSourceMapper.selectById(900001L)).thenReturn(source);
            when(reconciliationMapper.countImportedFile(eq(1L), eq("MANGO_PAY"), eq(billDate), anyString()))
                    .thenReturn(0L);
            PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
            paymentOrder.setId(200001L);
            paymentOrder.setPayOrderNo("PO001");
            paymentOrder.setBusinessOrderId(100001L);
            paymentOrder.setAmount(9900L);
            paymentOrder.setStatus("SUCCESS");
            when(paymentOrderMapper.selectByTenantAndChannelTradeNo(1L, "MANGO_PAY", "FTP-PO001")).thenReturn(paymentOrder);
            when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("MATCHED"));
            when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

            FetchPaymentChannelBillCommand command = new FetchPaymentChannelBillCommand();
            command.setSourceId(900001L);
            command.setBillDate(billDate);

            PaymentReconciliationVO result = service.fetchChannelBill(command);

            assertThat(result.getMatchStatusName()).isEqualTo("已平账");
            ArgumentCaptor<PaymentChannelBillFetchBatchEntity> fetchBatchCaptor = ArgumentCaptor.forClass(PaymentChannelBillFetchBatchEntity.class);
            verify(billFetchBatchMapper).insert(fetchBatchCaptor.capture());
            assertThat(fetchBatchCaptor.getValue().getFetchMode()).isEqualTo("FTP");
            ArgumentCaptor<PaymentChannelBillDetailEntity> detailCaptor = ArgumentCaptor.forClass(PaymentChannelBillDetailEntity.class);
            verify(billDetailMapper).insert(detailCaptor.capture());
            assertThat(detailCaptor.getValue().getChannelTradeNo()).isEqualTo("FTP-PO001");
            assertThat(detailCaptor.getValue().getMatchStatus()).isEqualTo("MATCHED");
            ArgumentCaptor<PaymentReconciliationEntity> reconciliationCaptor = ArgumentCaptor.forClass(PaymentReconciliationEntity.class);
            verify(reconciliationMapper).insert(reconciliationCaptor.capture());
            assertThat(reconciliationCaptor.getValue().getBillFileName()).isEqualTo("2026-06-06.json");
            assertThat(reconciliationCaptor.getValue().getTotalAmount()).isEqualTo(9900L);
            assertThat(reconciliationCaptor.getValue().getTotalFee()).isEqualTo(12L);
            ArgumentCaptor<PaymentChannelBillFetchBatchEntity> updateBatchCaptor = ArgumentCaptor.forClass(PaymentChannelBillFetchBatchEntity.class);
            verify(billFetchBatchMapper).updateById(updateBatchCaptor.capture());
            assertThat(updateBatchCaptor.getValue().getFetchStatus()).isEqualTo("SUCCESS");
            assertThat(updateBatchCaptor.getValue().getFetchResult()).isEqualTo("FTP 拉取通道账单获取并对账完成");
            assertThat(updateBatchCaptor.getValue().getTotalCount()).isEqualTo(1);
            assertThat(ftpServer.awaitRequest()).isTrue();
        }
    }

    @Test
    @DisplayName("fetchChannelBill should reject FTP credential reference when no provider resolves it")
    void fetchChannelBill_ftpCredentialRefWithoutProvider_rejectsBeforeImport() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        try (SimpleFtpServer ftpServer = new SimpleFtpServer("/bills/2026-06-06.json", "{\"items\":[]}")) {
            PaymentChannelBillSourceEntity source = new PaymentChannelBillSourceEntity();
            source.setId(900002L);
            source.setTenantId(1L);
            source.setDelFlag(0);
            source.setEnabled(1);
            source.setChannelCode("MANGO_PAY");
            source.setFetchMode("FTP");
            source.setEndpoint("127.0.0.1:" + ftpServer.port());
            source.setRemotePath("/bills/2026-06-06.json");
            source.setCredentialRef("credential:payment-bill-ftp");
            when(billSourceMapper.selectById(900002L)).thenReturn(source);

            FetchPaymentChannelBillCommand command = new FetchPaymentChannelBillCommand();
            command.setSourceId(900002L);
            command.setBillDate(billDate);

            assertThatThrownBy(() -> service.fetchChannelBill(command))
                    .hasMessageContaining("未找到 FTP/FTPS 认证配置引用");
            verify(reconciliationMapper, never()).insert(any(PaymentReconciliationEntity.class));
            verify(billDetailMapper, never()).insert(any(PaymentChannelBillDetailEntity.class));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    @DisplayName("generateMangoPayVirtualBill should use real payment and refund rows")
    void generateMangoPayVirtualBill_withRealRows_createsMatchedBatch() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.selectMangoPayBillItems(1L, "MANGO_PAY", billDate, billDate.plusDays(1)))
                .thenReturn(List.of(
                        billRow("CASHIER-PO001", "PAYMENT", 9900L, 12L, LocalDateTime.of(2026, 6, 6, 10, 0)),
                        billRow("CASHIER-REFUND-RO001", "REFUND", 3900L, 8L, LocalDateTime.of(2026, 6, 6, 11, 0))));
        when(reconciliationMapper.countImportedFile(eq(1L), eq("MANGO_PAY"), eq(billDate), anyString()))
                .thenReturn(0L);
        PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
        paymentOrder.setId(200001L);
        paymentOrder.setPayOrderNo("PO001");
        paymentOrder.setBusinessOrderId(100001L);
        paymentOrder.setAmount(9900L);
        paymentOrder.setStatus("SUCCESS");
        when(paymentOrderMapper.selectByTenantAndChannelTradeNo(1L, "MANGO_PAY", "CASHIER-PO001")).thenReturn(paymentOrder);
        PaymentRefundOrderEntity refundOrder = new PaymentRefundOrderEntity();
        refundOrder.setId(300001L);
        refundOrder.setRefundOrderNo("RO001");
        refundOrder.setPaymentOrderId(200001L);
        refundOrder.setRefundAmount(3900L);
        refundOrder.setStatus("SUCCESS");
        when(refundOrderMapper.selectEntityByTenantAndChannelRefundNo(1L, "CASHIER-REFUND-RO001")).thenReturn(refundOrder);
        when(paymentOrderMapper.selectEntityByTenantAndId(1L, 200001L)).thenReturn(paymentOrder);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("MATCHED"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        GenerateMangoPayVirtualBillCommand command = new GenerateMangoPayVirtualBillCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);

        PaymentReconciliationVO result = service.generateMangoPayVirtualBill(command);

        assertThat(result.getMatchStatusName()).isEqualTo("已平账");
        ArgumentCaptor<PaymentReconciliationEntity> reconciliationCaptor = ArgumentCaptor.forClass(PaymentReconciliationEntity.class);
        verify(reconciliationMapper).insert(reconciliationCaptor.capture());
        PaymentReconciliationEntity reconciliation = reconciliationCaptor.getValue();
        assertThat(reconciliation.getChannelCode()).isEqualTo("MANGO_PAY");
        assertThat(reconciliation.getBillFileName()).isEqualTo("MANGO_PAY-2026-06-06-generated.bill");
        assertThat(reconciliation.getTotalCount()).isEqualTo(2);
        assertThat(reconciliation.getTotalAmount()).isEqualTo(13800L);
        assertThat(reconciliation.getTotalFee()).isEqualTo(20L);
        assertThat(reconciliation.getMatchStatus()).isEqualTo("MATCHED");
        ArgumentCaptor<PaymentChannelBillDetailEntity> detailCaptor = ArgumentCaptor.forClass(PaymentChannelBillDetailEntity.class);
        verify(billDetailMapper, org.mockito.Mockito.times(2)).insert(detailCaptor.capture());
        assertThat(detailCaptor.getAllValues()).extracting(PaymentChannelBillDetailEntity::getTradeType)
                .containsExactly("PAYMENT", "REFUND");
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);
        verify(transactionFlowMapper, org.mockito.Mockito.times(2)).insert(flowCaptor.capture());
        assertThat(flowCaptor.getAllValues())
                .extracting(PaymentTransactionFlowEntity::getFlowType)
                .containsExactly("CHANNEL_FEE", "CHANNEL_FEE");
        assertThat(flowCaptor.getAllValues())
                .extracting(PaymentTransactionFlowEntity::getAmount)
                .containsExactly(12L, 8L);
        assertThat(flowCaptor.getAllValues().get(0).getBusinessOrderId()).isEqualTo(100001L);
        assertThat(flowCaptor.getAllValues().get(0).getPaymentOrderId()).isEqualTo(200001L);
        assertThat(flowCaptor.getAllValues().get(0).getRefundOrderId()).isNull();
        assertThat(flowCaptor.getAllValues().get(1).getBusinessOrderId()).isEqualTo(100001L);
        assertThat(flowCaptor.getAllValues().get(1).getPaymentOrderId()).isEqualTo(200001L);
        assertThat(flowCaptor.getAllValues().get(1).getRefundOrderId()).isEqualTo(300001L);
        verify(auditService).record(
                eq(PaymentOperationAuditService.ACTION_GENERATE_MANGO_PAY_CHANNEL_BILL),
                eq(PaymentOperationAuditService.RESOURCE_PAYMENT_RECONCILIATION),
                eq(reconciliation.getReconciliationNo()),
                eq(PaymentOperationAuditService.RESULT_SUCCESS));
        verify(observabilityService).logSummary(
                eq("RECONCILIATION_BATCH"),
                eq(reconciliation.getReconciliationNo()),
                eq("MATCHED"),
                eq(13800L),
                eq("MANGO_PAY"),
                any(Long.class),
                eq(PaymentOperationAuditService.RESULT_SUCCESS));
    }

    @Test
    @DisplayName("generateMangoPayVirtualBill should reject duplicate generated bill")
    void generateMangoPayVirtualBill_duplicateDigest_rejects() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.selectMangoPayBillItems(1L, "MANGO_PAY", billDate, billDate.plusDays(1)))
                .thenReturn(List.of(billRow("CASHIER-PO001", "PAYMENT", 9900L, 0L, LocalDateTime.of(2026, 6, 6, 10, 0))));
        when(reconciliationMapper.countImportedFile(eq(1L), eq("MANGO_PAY"), eq(billDate), anyString()))
                .thenReturn(1L);

        GenerateMangoPayVirtualBillCommand command = new GenerateMangoPayVirtualBillCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);

        assertThatThrownBy(() -> service.generateMangoPayVirtualBill(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_RECONCILIATION_FILE_DUPLICATED.getMessage());
        verify(reconciliationMapper, never()).insert(any(PaymentReconciliationEntity.class));
        verify(billDetailMapper, never()).insert(any(PaymentChannelBillDetailEntity.class));
    }

    @Test
    @DisplayName("generateMangoPayVirtualBill should apply next bill difference control")
    void generateMangoPayVirtualBill_billDifferenceControl_createsAmountDifference() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.selectMangoPayBillItems(1L, "MANGO_PAY", billDate, billDate.plusDays(1)))
                .thenReturn(List.of(billRow("CASHIER-PO001", "PAYMENT", 9900L, 0L, LocalDateTime.of(2026, 6, 6, 10, 0))));
        PaymentMangoPayScenarioControl scenario = new PaymentMangoPayScenarioControl();
        scenario.setBillDifferenceType("AMOUNT_PLUS");
        scenario.setDifferenceAmount(100L);
        when(scenarioControlService.consumeBillScenario(331001L)).thenReturn(scenario);
        when(reconciliationMapper.countImportedFile(eq(1L), eq("MANGO_PAY"), eq(billDate), anyString()))
                .thenReturn(0L);
        PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
        paymentOrder.setPayOrderNo("PO001");
        paymentOrder.setAmount(9900L);
        paymentOrder.setStatus("SUCCESS");
        when(paymentOrderMapper.selectByTenantAndChannelTradeNo(1L, "MANGO_PAY", "CASHIER-PO001")).thenReturn(paymentOrder);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("DIFFERENCE"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        GenerateMangoPayVirtualBillCommand command = new GenerateMangoPayVirtualBillCommand();
        command.setChannelCode("MANGO_PAY");
        command.setContractId(331001L);
        command.setBillDate(billDate);

        PaymentReconciliationVO result = service.generateMangoPayVirtualBill(command);

        assertThat(result.getMatchStatusName()).isEqualTo("存在差异");
        ArgumentCaptor<PaymentChannelBillDetailEntity> detailCaptor = ArgumentCaptor.forClass(PaymentChannelBillDetailEntity.class);
        verify(billDetailMapper).insert(detailCaptor.capture());
        assertThat(detailCaptor.getValue().getAmount()).isEqualTo(10000L);
        ArgumentCaptor<PaymentDifferenceEntity> differenceCaptor = ArgumentCaptor.forClass(PaymentDifferenceEntity.class);
        verify(differenceMapper).insert(differenceCaptor.capture());
        assertThat(differenceCaptor.getValue().getDifferenceType()).isEqualTo("AMOUNT_MISMATCH");
        assertThat(differenceCaptor.getValue().getDifferenceAmount()).isEqualTo(100L);
    }

    @Test
    @DisplayName("importReconciliation should match refund bill amount")
    void importReconciliation_refundAmountMismatch_createsDifference() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.countImportedFile(1L, "MANGO_PAY", billDate, "digest-refund"))
                .thenReturn(0L);
        PaymentRefundOrderEntity refundOrder = new PaymentRefundOrderEntity();
        refundOrder.setRefundOrderNo("RO001");
        refundOrder.setRefundAmount(3900L);
        refundOrder.setStatus("SUCCESS");
        when(refundOrderMapper.selectEntityByTenantAndChannelRefundNo(1L, "CASHIER-REFUND-RO001")).thenReturn(refundOrder);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("DIFFERENCE"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        ImportPaymentReconciliationCommand command = new ImportPaymentReconciliationCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);
        command.setBillFileName("refund.bill");
        command.setFileDigest("digest-refund");
        ImportPaymentReconciliationCommand.BillItem item = new ImportPaymentReconciliationCommand.BillItem();
        item.setChannelTradeNo("CASHIER-REFUND-RO001");
        item.setTradeType("refund");
        item.setAmount(4000L);
        item.setFee(0L);
        item.setTradeTime(LocalDateTime.of(2026, 6, 6, 11, 0));
        command.setItems(List.of(item));

        PaymentReconciliationVO result = service.importReconciliation(command);

        assertThat(result.getMatchStatusName()).isEqualTo("存在差异");
        ArgumentCaptor<PaymentDifferenceEntity> differenceCaptor = ArgumentCaptor.forClass(PaymentDifferenceEntity.class);
        verify(differenceMapper).insert(differenceCaptor.capture());
        PaymentDifferenceEntity difference = differenceCaptor.getValue();
        assertThat(difference.getRelatedOrderNo()).isEqualTo("RO001");
        assertThat(difference.getDifferenceType()).isEqualTo("REFUND_AMOUNT_MISMATCH");
        assertThat(difference.getDifferenceAmount()).isEqualTo(100L);
    }

    @Test
    @DisplayName("importReconciliation should compensate paying payment order when channel bill proves success")
    void importReconciliation_payingPaymentBillMatched_compensatesSuccessState() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.countImportedFile(1L, "MANGO_PAY", billDate, "digest-payment-compensate"))
                .thenReturn(0L);
        PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
        paymentOrder.setId(200001L);
        paymentOrder.setPayOrderNo("PO-COMP");
        paymentOrder.setBusinessOrderId(100001L);
        paymentOrder.setAmount(9900L);
        paymentOrder.setStatus("PAYING");
        when(paymentOrderMapper.selectByTenantAndChannelTradeNo(1L, "MANGO_PAY", "CASHIER-PO-COMP"))
                .thenReturn(paymentOrder);
        when(paymentOrderMapper.updatePayingQueryResult(eq(1L), eq(200001L), eq("SUCCESS"), eq(1), any()))
                .thenReturn(1);
        when(businessOrderMapper.markCashierPaySuccess(1L, 100001L, 9900L)).thenReturn(1);
        PaymentBusinessOrderEntity businessOrder = new PaymentBusinessOrderEntity();
        businessOrder.setBizOrderNo("BO-COMP");
        businessOrder.setStatus("PAYING");
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 100001L)).thenReturn(businessOrder);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("MATCHED"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        ImportPaymentReconciliationCommand command = new ImportPaymentReconciliationCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);
        command.setBillFileName("payment-compensate.bill");
        command.setFileDigest("digest-payment-compensate");
        command.setItems(List.of(billItem(
                "CASHIER-PO-COMP", "payment", 9900L, 0L, LocalDateTime.of(2026, 6, 6, 10, 0))));

        PaymentReconciliationVO result = service.importReconciliation(command);

        assertThat(result.getMatchStatusName()).isEqualTo("已平账");
        verify(paymentOrderMapper).updatePayingQueryResult(eq(1L), eq(200001L), eq("SUCCESS"), eq(1), eq(LocalDateTime.of(2026, 6, 6, 10, 0)));
        verify(businessOrderMapper).markCashierPaySuccess(1L, 100001L, 9900L);
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getFlowType()).isEqualTo("PAY_SUCCESS");
        assertThat(flowCaptor.getValue().getAmount()).isEqualTo(9900L);
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT),
                eq(200001L),
                eq("PO-COMP"),
                eq("PAYING"),
                eq("SUCCESS"),
                eq(PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE),
                eq("CASHIER-PO-COMP"),
                any(),
                eq("对账账单确认支付成功并补偿推进支付订单状态"));
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS),
                eq(100001L),
                eq("BO-COMP"),
                eq("PAYING"),
                eq("PAID"),
                eq(PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE),
                eq("CASHIER-PO-COMP"),
                any(),
                eq("对账账单确认支付成功并补偿推进业务订单状态"));
        verify(differenceMapper, never()).insert(any(PaymentDifferenceEntity.class));
    }

    @Test
    @DisplayName("importReconciliation should not compensate paying payment order when amount mismatches")
    void importReconciliation_payingPaymentAmountMismatch_createsDifferenceOnly() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.countImportedFile(1L, "MANGO_PAY", billDate, "digest-payment-mismatch"))
                .thenReturn(0L);
        PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
        paymentOrder.setId(200001L);
        paymentOrder.setPayOrderNo("PO-MISMATCH");
        paymentOrder.setBusinessOrderId(100001L);
        paymentOrder.setAmount(9900L);
        paymentOrder.setStatus("PAYING");
        when(paymentOrderMapper.selectByTenantAndChannelTradeNo(1L, "MANGO_PAY", "CASHIER-PO-MISMATCH"))
                .thenReturn(paymentOrder);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("DIFFERENCE"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        ImportPaymentReconciliationCommand command = new ImportPaymentReconciliationCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);
        command.setBillFileName("payment-mismatch.bill");
        command.setFileDigest("digest-payment-mismatch");
        command.setItems(List.of(billItem(
                "CASHIER-PO-MISMATCH", "payment", 10000L, 0L, LocalDateTime.of(2026, 6, 6, 10, 0))));

        PaymentReconciliationVO result = service.importReconciliation(command);

        assertThat(result.getMatchStatusName()).isEqualTo("存在差异");
        verify(paymentOrderMapper, never()).updatePayingQueryResult(any(), any(), anyString(), any(), any());
        verify(businessOrderMapper, never()).markCashierPaySuccess(any(), any(), any());
        verify(statusFlowService, never()).record(any(), anyString(), any(), anyString(), any(), anyString(), anyString(), anyString(), any(), anyString());
        ArgumentCaptor<PaymentDifferenceEntity> differenceCaptor = ArgumentCaptor.forClass(PaymentDifferenceEntity.class);
        verify(differenceMapper).insert(differenceCaptor.capture());
        assertThat(differenceCaptor.getValue().getDifferenceType()).isEqualTo("STATUS_MISMATCH");
        assertThat(differenceCaptor.getValue().getRelatedOrderNo()).isEqualTo("PO-MISMATCH");
    }

    @Test
    @DisplayName("importReconciliation should compensate refunding order when channel bill proves success")
    void importReconciliation_refundingBillMatched_compensatesSuccessState() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.countImportedFile(1L, "MANGO_PAY", billDate, "digest-refund-compensate"))
                .thenReturn(0L);
        PaymentRefundOrderEntity refundOrder = new PaymentRefundOrderEntity();
        refundOrder.setId(300001L);
        refundOrder.setRefundOrderNo("RO-COMP");
        refundOrder.setPaymentOrderId(200001L);
        refundOrder.setRefundAmount(3900L);
        refundOrder.setStatus("REFUNDING");
        when(refundOrderMapper.selectEntityByTenantAndChannelRefundNo(1L, "CASHIER-REFUND-COMP"))
                .thenReturn(refundOrder);
        PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
        paymentOrder.setId(200001L);
        paymentOrder.setBusinessOrderId(100001L);
        paymentOrder.setAmount(9900L);
        paymentOrder.setStatus("SUCCESS");
        when(paymentOrderMapper.selectEntityByTenantAndId(1L, 200001L)).thenReturn(paymentOrder);
        when(refundOrderMapper.updateRefundingQueryResult(eq(1L), eq(300001L), eq("SUCCESS"), any())).thenReturn(1);
        PaymentBusinessOrderEntity businessOrder = new PaymentBusinessOrderEntity();
        businessOrder.setBizOrderNo("BO-COMP");
        businessOrder.setStatus("SUCCESS");
        businessOrder.setPaidAmount(9900L);
        businessOrder.setRefundedAmount(0L);
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 100001L)).thenReturn(businessOrder);
        when(businessOrderMapper.updateRefundProgress(1L, 100001L, 3900L)).thenReturn(1);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("MATCHED"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        ImportPaymentReconciliationCommand command = new ImportPaymentReconciliationCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);
        command.setBillFileName("refund-compensate.bill");
        command.setFileDigest("digest-refund-compensate");
        command.setItems(List.of(billItem(
                "CASHIER-REFUND-COMP", "refund", 3900L, 0L, LocalDateTime.of(2026, 6, 6, 11, 0))));

        PaymentReconciliationVO result = service.importReconciliation(command);

        assertThat(result.getMatchStatusName()).isEqualTo("已平账");
        verify(refundOrderMapper).updateRefundingQueryResult(eq(1L), eq(300001L), eq("SUCCESS"), eq(LocalDateTime.of(2026, 6, 6, 11, 0)));
        verify(businessOrderMapper).updateRefundProgress(1L, 100001L, 3900L);
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getFlowType()).isEqualTo("REFUND_SUCCESS");
        assertThat(flowCaptor.getValue().getAmount()).isEqualTo(3900L);
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_REFUND),
                eq(300001L),
                eq("RO-COMP"),
                eq("REFUNDING"),
                eq("SUCCESS"),
                eq(PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE),
                eq("CASHIER-REFUND-COMP"),
                any(),
                eq("对账账单确认退款成功并补偿推进退款订单状态"));
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS),
                eq(100001L),
                eq("BO-COMP"),
                eq("SUCCESS"),
                eq("PARTIAL_REFUNDED"),
                eq(PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE),
                eq("CASHIER-REFUND-COMP"),
                any(),
                eq("对账账单确认退款成功并补偿推进业务订单状态"));
        verify(differenceMapper, never()).insert(any(PaymentDifferenceEntity.class));
    }

    @Test
    @DisplayName("importReconciliation should create differences for local successful orders missing from channel bill")
    void importReconciliation_localSuccessfulOrdersMissingFromBill_createsDifferences() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.countImportedFile(1L, "MANGO_PAY", billDate, "digest-local-missing"))
                .thenReturn(0L);
        PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
        paymentOrder.setPayOrderNo("PO-MISSING");
        paymentOrder.setAmount(9900L);
        when(paymentOrderMapper.selectSuccessfulChannelOrdersMissingInBill(
                1L,
                "MANGO_PAY",
                billDate,
                billDate.plusDays(1),
                List.of("CASHIER-PO001")))
                .thenReturn(List.of(paymentOrder));
        PaymentRefundOrderEntity refundOrder = new PaymentRefundOrderEntity();
        refundOrder.setRefundOrderNo("RO-MISSING");
        refundOrder.setRefundAmount(3900L);
        when(refundOrderMapper.selectSuccessfulChannelRefundsMissingInBill(
                1L,
                "MANGO_PAY",
                billDate,
                billDate.plusDays(1),
                List.of()))
                .thenReturn(List.of(refundOrder));
        PaymentOrderEntity matchedOrder = new PaymentOrderEntity();
        matchedOrder.setId(200001L);
        matchedOrder.setPayOrderNo("PO001");
        matchedOrder.setBusinessOrderId(100001L);
        matchedOrder.setAmount(9900L);
        matchedOrder.setStatus("SUCCESS");
        when(paymentOrderMapper.selectByTenantAndChannelTradeNo(1L, "MANGO_PAY", "CASHIER-PO001")).thenReturn(matchedOrder);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("DIFFERENCE"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        ImportPaymentReconciliationCommand command = new ImportPaymentReconciliationCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);
        command.setBillFileName("local-missing.bill");
        command.setFileDigest("digest-local-missing");
        command.setItems(List.of(billItem(
                "CASHIER-PO001", "payment", 9900L, 0L, LocalDateTime.of(2026, 6, 6, 10, 0))));

        PaymentReconciliationVO result = service.importReconciliation(command);

        assertThat(result.getMatchStatusName()).isEqualTo("存在差异");
        ArgumentCaptor<PaymentDifferenceEntity> differenceCaptor = ArgumentCaptor.forClass(PaymentDifferenceEntity.class);
        verify(differenceMapper, org.mockito.Mockito.times(2)).insert(differenceCaptor.capture());
        assertThat(differenceCaptor.getAllValues())
                .extracting(PaymentDifferenceEntity::getDifferenceType)
                .containsExactly("LOCAL_SUCCESS_CHANNEL_MISSING", "LOCAL_REFUND_CHANNEL_MISSING");
        assertThat(differenceCaptor.getAllValues())
                .extracting(PaymentDifferenceEntity::getRelatedOrderNo)
                .containsExactly("PO-MISSING", "RO-MISSING");
        assertThat(differenceCaptor.getAllValues())
                .extracting(PaymentDifferenceEntity::getDifferenceAmount)
                .containsExactly(9900L, 3900L);
    }

    @Test
    @DisplayName("importReconciliation should not duplicate existing channel fee flow")
    void importReconciliation_existingChannelFeeFlow_doesNotDuplicate() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.countImportedFile(1L, "MANGO_PAY", billDate, "digest-fee"))
                .thenReturn(0L);
        PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
        paymentOrder.setId(200001L);
        paymentOrder.setPayOrderNo("PO001");
        paymentOrder.setBusinessOrderId(100001L);
        paymentOrder.setAmount(9900L);
        paymentOrder.setStatus("SUCCESS");
        when(paymentOrderMapper.selectByTenantAndChannelTradeNo(1L, "MANGO_PAY", "CASHIER-PO001")).thenReturn(paymentOrder);
        PaymentTransactionFlowEntity existing = new PaymentTransactionFlowEntity();
        existing.setAmount(12L);
        when(transactionFlowMapper.selectChannelFeeFlowByPaymentOrder(1L, 200001L)).thenReturn(existing);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("MATCHED"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        ImportPaymentReconciliationCommand command = new ImportPaymentReconciliationCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);
        command.setBillFileName("fee.bill");
        command.setFileDigest("digest-fee");
        command.setItems(List.of(billItem(
                "CASHIER-PO001", "payment", 9900L, 12L, LocalDateTime.of(2026, 6, 6, 10, 0))));

        PaymentReconciliationVO result = service.importReconciliation(command);

        assertThat(result.getMatchStatusName()).isEqualTo("已平账");
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(differenceMapper, never()).insert(any(PaymentDifferenceEntity.class));
    }

    @Test
    @DisplayName("importReconciliation should create difference when existing channel fee flow amount mismatches")
    void importReconciliation_existingChannelFeeFlowAmountMismatch_createsDifference() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.countImportedFile(1L, "MANGO_PAY", billDate, "digest-fee-mismatch"))
                .thenReturn(0L);
        PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
        paymentOrder.setId(200001L);
        paymentOrder.setPayOrderNo("PO001");
        paymentOrder.setBusinessOrderId(100001L);
        paymentOrder.setAmount(9900L);
        paymentOrder.setStatus("SUCCESS");
        when(paymentOrderMapper.selectByTenantAndChannelTradeNo(1L, "MANGO_PAY", "CASHIER-PO001")).thenReturn(paymentOrder);
        PaymentTransactionFlowEntity existing = new PaymentTransactionFlowEntity();
        existing.setAmount(10L);
        when(transactionFlowMapper.selectChannelFeeFlowByPaymentOrder(1L, 200001L)).thenReturn(existing);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("DIFFERENCE"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        ImportPaymentReconciliationCommand command = new ImportPaymentReconciliationCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);
        command.setBillFileName("fee-mismatch.bill");
        command.setFileDigest("digest-fee-mismatch");
        command.setItems(List.of(billItem(
                "CASHIER-PO001", "payment", 9900L, 12L, LocalDateTime.of(2026, 6, 6, 10, 0))));

        PaymentReconciliationVO result = service.importReconciliation(command);

        assertThat(result.getMatchStatusName()).isEqualTo("存在差异");
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        ArgumentCaptor<PaymentDifferenceEntity> differenceCaptor = ArgumentCaptor.forClass(PaymentDifferenceEntity.class);
        verify(differenceMapper).insert(differenceCaptor.capture());
        assertThat(differenceCaptor.getValue().getRelatedOrderNo()).isEqualTo("PO001");
        assertThat(differenceCaptor.getValue().getDifferenceType()).isEqualTo("CHANNEL_FEE_MISMATCH");
        assertThat(differenceCaptor.getValue().getDifferenceAmount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("importReconciliation should not create channel fee flow for standalone fee bill item")
    void importReconciliation_standaloneFeeBillItem_doesNotCreateFlow() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.countImportedFile(1L, "MANGO_PAY", billDate, "digest-standalone-fee"))
                .thenReturn(0L);
        when(reconciliationMapper.selectReconciliationDetail(eq(1L), any())).thenReturn(reconciliationVO("MATCHED"));
        when(billDetailMapper.selectBillDetails(eq(1L), any())).thenReturn(List.of());

        ImportPaymentReconciliationCommand command = new ImportPaymentReconciliationCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);
        command.setBillFileName("standalone-fee.bill");
        command.setFileDigest("digest-standalone-fee");
        command.setItems(List.of(billItem(
                "MANGO-FEE-001", "fee", 0L, 12L, LocalDateTime.of(2026, 6, 6, 23, 0))));

        PaymentReconciliationVO result = service.importReconciliation(command);

        assertThat(result.getMatchStatusName()).isEqualTo("已平账");
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(differenceMapper, never()).insert(any(PaymentDifferenceEntity.class));
    }

    @Test
    @DisplayName("importReconciliation should reject amount total overflow through money boundary")
    void importReconciliation_amountTotalOverflow_rejects() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.countImportedFile(1L, "MANGO_PAY", billDate, "digest-overflow"))
                .thenReturn(0L);

        ImportPaymentReconciliationCommand command = new ImportPaymentReconciliationCommand();
        command.setChannelCode("MANGO_PAY");
        command.setBillDate(billDate);
        command.setBillFileName("overflow.bill");
        command.setFileDigest("digest-overflow");
        ImportPaymentReconciliationCommand.BillItem first = billItem(
                "CASHIER-PO001", "payment", Long.MAX_VALUE, 0L, LocalDateTime.of(2026, 6, 6, 10, 0));
        ImportPaymentReconciliationCommand.BillItem second = billItem(
                "CASHIER-PO002", "payment", 1L, 0L, LocalDateTime.of(2026, 6, 6, 10, 1));
        command.setItems(List.of(first, second));

        assertThatThrownBy(() -> service.importReconciliation(command))
                .isInstanceOf(BizException.class)
                .hasMessage("金额超过系统支持上限");
    }

    @Test
    @DisplayName("generateMangoPayVirtualBill should reject plus difference overflow through money boundary")
    void generateMangoPayVirtualBill_billDifferenceOverflow_rejects() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.selectMangoPayBillItems(1L, "MANGO_PAY", billDate, billDate.plusDays(1)))
                .thenReturn(List.of(billRow("CASHIER-PO001", "PAYMENT", Long.MAX_VALUE, 0L, LocalDateTime.of(2026, 6, 6, 10, 0))));
        PaymentMangoPayScenarioControl scenario = new PaymentMangoPayScenarioControl();
        scenario.setBillDifferenceType("AMOUNT_PLUS");
        scenario.setDifferenceAmount(1L);
        when(scenarioControlService.consumeBillScenario(331001L)).thenReturn(scenario);

        GenerateMangoPayVirtualBillCommand command = new GenerateMangoPayVirtualBillCommand();
        command.setChannelCode("MANGO_PAY");
        command.setContractId(331001L);
        command.setBillDate(billDate);

        assertThatThrownBy(() -> service.generateMangoPayVirtualBill(command))
                .isInstanceOf(BizException.class)
                .hasMessage("金额超过系统支持上限");
    }

    private PaymentChannelBillItemRow billRow(
            String channelTradeNo,
            String tradeType,
            Long amount,
            Long fee,
            LocalDateTime tradeTime) {
        PaymentChannelBillItemRow row = new PaymentChannelBillItemRow();
        row.setChannelTradeNo(channelTradeNo);
        row.setTradeType(tradeType);
        row.setAmount(amount);
        row.setFee(fee);
        row.setTradeTime(tradeTime);
        return row;
    }

    private ImportPaymentReconciliationCommand.BillItem billItem(
            String channelTradeNo,
            String tradeType,
            Long amount,
            Long fee,
            LocalDateTime tradeTime) {
        ImportPaymentReconciliationCommand.BillItem item = new ImportPaymentReconciliationCommand.BillItem();
        item.setChannelTradeNo(channelTradeNo);
        item.setTradeType(tradeType);
        item.setAmount(amount);
        item.setFee(fee);
        item.setTradeTime(tradeTime);
        return item;
    }

    private PaymentReconciliationVO reconciliationVO(String status) {
        PaymentReconciliationVO vo = new PaymentReconciliationVO();
        vo.setId(500001L);
        vo.setMatchStatus(status);
        return vo;
    }

    private static class SimpleFtpServer implements AutoCloseable {

        private final String remotePath;
        private final String body;
        private final ServerSocket controlSocket;
        private final CountDownLatch requestLatch = new CountDownLatch(1);
        private final Thread worker;
        private volatile boolean running = true;

        SimpleFtpServer(String remotePath, String body) throws IOException {
            this.remotePath = remotePath;
            this.body = body;
            this.controlSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            this.worker = new Thread(this::serve, "payment-test-ftp-server");
            this.worker.start();
        }

        int port() {
            return controlSocket.getLocalPort();
        }

        boolean awaitRequest() throws InterruptedException {
            return requestLatch.await(5, TimeUnit.SECONDS);
        }

        private void serve() {
            try (Socket socket = controlSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 OutputStream output = socket.getOutputStream()) {
                ServerSocket dataSocket = null;
                reply(output, "220 Mango payment test FTP ready");
                String line;
                while (running && (line = reader.readLine()) != null) {
                    String command = line.toUpperCase(java.util.Locale.ROOT);
                    if (command.startsWith("USER")) {
                        reply(output, "331 Password required");
                    } else if (command.startsWith("PASS")) {
                        reply(output, "230 Login successful");
                    } else if (command.startsWith("TYPE")) {
                        reply(output, "200 Type set");
                    } else if (command.startsWith("SYST")) {
                        reply(output, "215 UNIX Type: L8");
                    } else if (command.startsWith("PWD")) {
                        reply(output, "257 \"/\" is current directory");
                    } else if (command.startsWith("PASV")) {
                        closeDataSocket(dataSocket);
                        dataSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
                        int dataPort = dataSocket.getLocalPort();
                        reply(output, "227 Entering Passive Mode (127,0,0,1," + (dataPort / 256) + "," + (dataPort % 256) + ")");
                    } else if (command.startsWith("RETR")) {
                        String requestedPath = line.substring(4).trim();
                        if (!remotePath.equals(requestedPath) || dataSocket == null) {
                            reply(output, "550 File unavailable");
                            continue;
                        }
                        reply(output, "150 Opening binary mode data connection");
                        try (Socket data = dataSocket.accept()) {
                            data.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            data.getOutputStream().flush();
                        }
                        requestLatch.countDown();
                        reply(output, "226 Transfer complete");
                    } else if (command.startsWith("QUIT")) {
                        reply(output, "221 Goodbye");
                        break;
                    } else {
                        reply(output, "200 OK");
                    }
                }
                closeDataSocket(dataSocket);
            } catch (IOException ignored) {
            }
        }

        private void reply(OutputStream output, String line) throws IOException {
            output.write((line + "\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            output.flush();
        }

        private void closeDataSocket(ServerSocket dataSocket) throws IOException {
            if (dataSocket != null && !dataSocket.isClosed()) {
                dataSocket.close();
            }
        }

        @Override
        public void close() throws Exception {
            running = false;
            controlSocket.close();
            worker.join(TimeUnit.SECONDS.toMillis(2));
        }
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
