package io.mango.payment.core.service;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.command.FetchPaymentChannelBillCommand;
import io.mango.payment.core.entity.PaymentChannelBillSourceEntity;
import io.mango.payment.core.mapper.PaymentChannelBillFetchBatchMapper;
import io.mango.payment.core.mapper.PaymentChannelBillSourceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentChannelBillFetchScheduleServiceTest {

    private PaymentChannelBillSourceMapper billSourceMapper;
    private PaymentChannelBillFetchBatchMapper billFetchBatchMapper;
    private PaymentReconciliationService reconciliationService;
    private PaymentChannelBillFetchScheduleService service;

    @BeforeEach
    void setUp() {
        billSourceMapper = mock(PaymentChannelBillSourceMapper.class);
        billFetchBatchMapper = mock(PaymentChannelBillFetchBatchMapper.class);
        reconciliationService = mock(PaymentReconciliationService.class);
        service = new PaymentChannelBillFetchScheduleService(billSourceMapper, billFetchBatchMapper, reconciliationService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("fetchScheduledChannelBills should fetch enabled automatic sources and skip successful source")
    void fetchScheduledChannelBills_fetchesEnabledAutomaticSourcesAndSkipsSuccessfulSource() {
        LocalDate billDate = LocalDate.of(2026, 6, 10);
        PaymentChannelBillSourceEntity skipped = source(101L, "MANGO_PAY", "HTTP");
        PaymentChannelBillSourceEntity fetched = source(102L, "OFFLINE_COLLECTION", "FTP");
        PaymentChannelBillSourceEntity failed = source(103L, "TONG_LIAN", "FTPS");
        when(billSourceMapper.selectEnabledAutomaticSources(1L)).thenReturn(List.of(skipped, fetched, failed));
        when(billFetchBatchMapper.countSuccessfulFetch(1L, 101L, billDate)).thenReturn(1L);
        when(billFetchBatchMapper.countSuccessfulFetch(1L, 102L, billDate)).thenReturn(0L);
        when(billFetchBatchMapper.countSuccessfulFetch(1L, 103L, billDate)).thenReturn(0L);
        when(reconciliationService.fetchChannelBill(any(FetchPaymentChannelBillCommand.class)))
                .thenReturn(null)
                .thenThrow(new IllegalArgumentException("远端账单不可用"));

        PaymentChannelBillFetchScheduleService.ScheduledBillFetchResult result =
                service.fetchScheduledChannelBills(billDate);

        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.messages()).contains("已跳过：MANGO_PAY/HTTP/2026-06-10");
        assertThat(result.messages()).anyMatch(message -> message.contains("失败：TONG_LIAN/FTPS"));
        ArgumentCaptor<FetchPaymentChannelBillCommand> commandCaptor =
                ArgumentCaptor.forClass(FetchPaymentChannelBillCommand.class);
        verify(reconciliationService, times(2)).fetchChannelBill(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues()).extracting(FetchPaymentChannelBillCommand::getSourceId)
                .containsExactly(102L, 103L);
        assertThat(commandCaptor.getAllValues()).extracting(FetchPaymentChannelBillCommand::getBillDate)
                .containsOnly(billDate);
    }

    private PaymentChannelBillSourceEntity source(Long id, String channelCode, String fetchMode) {
        PaymentChannelBillSourceEntity entity = new PaymentChannelBillSourceEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setChannelCode(channelCode);
        entity.setFetchMode(fetchMode);
        entity.setEnabled(1);
        entity.setDelFlag(0);
        return entity;
    }
}
