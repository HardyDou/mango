package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.job.api.enums.JobHandleStatus;
import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentChannelBillFetchJobHandlerTest {

    @Test
    @DisplayName("handle should use parameter billDate and return success when all sources succeed or skip")
    void handle_usesParameterBillDateAndReturnsSuccess() {
        PaymentChannelBillFetchScheduleService scheduleService = mock(PaymentChannelBillFetchScheduleService.class);
        PaymentChannelBillFetchJobHandler handler =
                new PaymentChannelBillFetchJobHandler(scheduleService, new ObjectMapper());
        LocalDate billDate = LocalDate.of(2026, 6, 10);
        when(scheduleService.fetchScheduledChannelBills(billDate)).thenReturn(
                new PaymentChannelBillFetchScheduleService.ScheduledBillFetchResult(
                        billDate, 2, 1, 0, 1, List.of("已跳过：MANGO_PAY/HTTP/2026-06-10")));
        MangoJobHandleContext context = new MangoJobHandleContext();
        context.setParameter("{\"billDate\":\"2026-06-10\"}");

        MangoJobHandleResult result = handler.handle(context);

        assertThat(result.getStatus()).isEqualTo(JobHandleStatus.SUCCESS);
        assertThat(result.getMessage()).contains("账单日 2026-06-10", "成功 1", "跳过 1", "失败 0");
        verify(scheduleService).fetchScheduledChannelBills(billDate);
        assertThat(handler.handlerName()).isEqualTo(PaymentChannelBillFetchJobHandler.HANDLER_NAME);
        assertThat(handler.supportedJobCodes()).containsExactly(PaymentChannelBillFetchJobHandler.JOB_CODE);
    }

    @Test
    @DisplayName("handle should return failed when scheduled fetch has failed sources")
    void handle_failedSources_returnsFailed() {
        PaymentChannelBillFetchScheduleService scheduleService = mock(PaymentChannelBillFetchScheduleService.class);
        PaymentChannelBillFetchJobHandler handler =
                new PaymentChannelBillFetchJobHandler(scheduleService, new ObjectMapper());
        LocalDate billDate = LocalDate.of(2026, 6, 10);
        when(scheduleService.fetchScheduledChannelBills(any())).thenReturn(
                new PaymentChannelBillFetchScheduleService.ScheduledBillFetchResult(
                        billDate, 1, 0, 1, 0, List.of("失败：TONG_LIAN/HTTP，远端不可用")));

        MangoJobHandleResult result = handler.handle(new MangoJobHandleContext());

        assertThat(result.getStatus()).isEqualTo(JobHandleStatus.FAILED);
        assertThat(result.getMessage()).contains("失败 1", "远端不可用");
        ArgumentCaptor<LocalDate> billDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(scheduleService).fetchScheduledChannelBills(billDateCaptor.capture());
        assertThat(billDateCaptor.getValue()).isEqualTo(LocalDate.now().minusDays(1));
    }

    @Test
    @DisplayName("handle should reject invalid billDate parameter")
    void handle_invalidBillDate_rejects() {
        PaymentChannelBillFetchScheduleService scheduleService = mock(PaymentChannelBillFetchScheduleService.class);
        PaymentChannelBillFetchJobHandler handler =
                new PaymentChannelBillFetchJobHandler(scheduleService, new ObjectMapper());
        MangoJobHandleContext context = new MangoJobHandleContext();
        context.setParameter("{\"billDate\":\"20260610\"}");

        assertThatThrownBy(() -> handler.handle(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("账单拉取任务参数 billDate 必须是 yyyy-MM-dd 格式");
    }
}
