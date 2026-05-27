package io.mango.notice.core.outbox;

import io.mango.infra.context.core.MangoContextHeaders;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.notice.core.service.INoticeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeOutboxDispatcherTest {

    private static final Instant NOW = Instant.parse("2026-05-26T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void dispatchOnce_batchSizeNotPositive_returnsZeroWithoutClaiming() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 0, 30, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(0, result);
        verify(outboxStore, never()).claim(any(), any(), any(Integer.class), any());
    }

    @Test
    void dispatchOnce_success_executesTaskAndAcks() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        OutboxMessage message = message(1001L, 1);
        when(outboxStore.claim("worker-a", NoticeOutboxMessageMapper.EVENT_TYPE, 20, NOW))
                .thenReturn(List.of(message));
        when(noticeService.hasRetryWaitingRecords(1001L)).thenReturn(false);
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 20, 30, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(1, result);
        verify(noticeService).executeTask(1001L);
        verify(outboxStore).ack(message.getMessageId(), "worker-a", NOW);
    }

    @Test
    void dispatchOnce_headerTenant_executesTaskWithinTenantContextAndRestoresOriginalContext() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        OutboxMessage message = OutboxMessage.builder()
                .messageId("notice-1001")
                .eventType(NoticeOutboxMessageMapper.EVENT_TYPE)
                .attemptCount(1)
                .payload(Map.of("taskId", 1001L))
                .headers(Map.of(MangoContextHeaders.TENANT_ID, "1"))
                .build();
        when(outboxStore.claim("worker-a", NoticeOutboxMessageMapper.EVENT_TYPE, 20, NOW))
                .thenReturn(List.of(message));
        when(noticeService.hasRetryWaitingRecords(1001L)).thenReturn(false);
        doAnswer(invocation -> {
            assertEquals("1", MangoContextHolder.tenantId());
            return 1;
        }).when(noticeService).executeTask(1001L);
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("origin"));
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 20, 30, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(1, result);
        ArgumentCaptor<Long> taskIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(noticeService).executeTask(taskIdCaptor.capture());
        assertEquals(1001L, taskIdCaptor.getValue());
        assertEquals("origin", MangoContextHolder.tenantId());
    }

    @Test
    void dispatchOnce_withoutHeaderTenant_usesTaskTenantFallback() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        OutboxMessage message = message(1001L, 1);
        when(outboxStore.claim("worker-a", NoticeOutboxMessageMapper.EVENT_TYPE, 20, NOW))
                .thenReturn(List.of(message));
        when(noticeService.findTaskTenantId(1001L)).thenReturn("1");
        when(noticeService.hasRetryWaitingRecords(1001L)).thenReturn(false);
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 20, 30, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(1, result);
        verify(noticeService).findTaskTenantId(1001L);
        verify(noticeService).executeTask(1001L);
        assertEquals(null, MangoContextHolder.tenantId());
    }

    @Test
    void toOutboxMessage_includesCurrentTenantHeader() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        OutboxMessage message = NoticeOutboxMessageMapper.toOutboxMessage(1001L, NOW);

        assertEquals("1", message.getHeaders().get(MangoContextHeaders.TENANT_ID));
    }

    @Test
    void dispatchOnce_retryWaitingBeforeMaxAttempts_nacksForRetry() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        OutboxMessage message = message(1001L, 1);
        when(outboxStore.claim("worker-a", NoticeOutboxMessageMapper.EVENT_TYPE, 20, NOW))
                .thenReturn(List.of(message));
        when(noticeService.hasRetryWaitingRecords(1001L)).thenReturn(true);
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 20, 30, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(1, result);
        verify(outboxStore).nack(
                message.getMessageId(),
                "worker-a",
                "通知任务仍有待重试记录",
                NOW.plusSeconds(30),
                NOW);
        verify(outboxStore, never()).ack(any(), any(), any());
    }

    @Test
    void dispatchOnce_retryWaitingAtMaxAttempts_finalizesAndAcks() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        OutboxMessage message = message(1001L, 3);
        when(outboxStore.claim("worker-a", NoticeOutboxMessageMapper.EVENT_TYPE, 20, NOW))
                .thenReturn(List.of(message));
        when(noticeService.hasRetryWaitingRecords(1001L)).thenReturn(true);
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 20, 30, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(1, result);
        verify(noticeService).finalizeRetryWaitingRecords(1001L, "通知任务达到最大重试次数");
        verify(outboxStore).ack(message.getMessageId(), "worker-a", NOW);
        verify(outboxStore, never()).nack(any(), any(), any(), any(), any());
    }

    @Test
    void dispatchOnce_executeTaskThrowsBeforeMaxAttempts_nacks() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        OutboxMessage message = message(1001L, 1);
        when(outboxStore.claim("worker-a", NoticeOutboxMessageMapper.EVENT_TYPE, 20, NOW))
                .thenReturn(List.of(message));
        doThrow(new IllegalStateException("temporary failure")).when(noticeService).executeTask(1001L);
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 20, 45, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(0, result);
        verify(outboxStore).nack(
                message.getMessageId(),
                "worker-a",
                "temporary failure",
                NOW.plusSeconds(45),
                NOW);
    }

    @Test
    void dispatchOnce_executeTaskThrowsAtMaxAttempts_finalizesAndAcks() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        OutboxMessage message = message(1001L, 3);
        when(outboxStore.claim("worker-a", NoticeOutboxMessageMapper.EVENT_TYPE, 20, NOW))
                .thenReturn(List.of(message));
        doThrow(new IllegalStateException("temporary failure")).when(noticeService).executeTask(1001L);
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 20, 45, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(1, result);
        verify(noticeService).finalizeRetryWaitingRecords(1001L, "temporary failure");
        verify(outboxStore).ack(message.getMessageId(), "worker-a", NOW);
        verify(outboxStore, never()).nack(any(), any(), any(), any(), any());
    }

    @Test
    void dispatchOnce_stringTaskId_executesTask() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        OutboxMessage message = OutboxMessage.builder()
                .messageId("notice-1001")
                .eventType(NoticeOutboxMessageMapper.EVENT_TYPE)
                .attemptCount(1)
                .payload(Map.of("taskId", "1001"))
                .build();
        when(outboxStore.claim("worker-a", NoticeOutboxMessageMapper.EVENT_TYPE, 20, NOW))
                .thenReturn(List.of(message));
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 20, 45, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(1, result);
        verify(noticeService).executeTask(1001L);
    }

    @Test
    void dispatchOnce_missingTaskId_nacksWithoutCrashingDispatcher() {
        IOutboxStore outboxStore = mock(IOutboxStore.class);
        INoticeService noticeService = mock(INoticeService.class);
        OutboxMessage message = OutboxMessage.builder()
                .messageId("notice-missing-task")
                .eventType(NoticeOutboxMessageMapper.EVENT_TYPE)
                .attemptCount(1)
                .payload(Map.of())
                .build();
        when(outboxStore.claim("worker-a", NoticeOutboxMessageMapper.EVENT_TYPE, 20, NOW))
                .thenReturn(List.of(message));
        NoticeOutboxDispatcher dispatcher = new NoticeOutboxDispatcher(
                outboxStore, noticeService, CLOCK, "worker-a", 20, 45, 3);

        int result = dispatcher.dispatchOnce();

        assertEquals(0, result);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxStore).nack(
                eq(message.getMessageId()),
                eq("worker-a"),
                errorCaptor.capture(),
                any(),
                any());
        assertEquals("通知 outbox 缺少 taskId", errorCaptor.getValue());
        verify(noticeService, never()).executeTask(any());
    }

    private OutboxMessage message(Long taskId, int attemptCount) {
        return OutboxMessage.builder()
                .messageId("notice-" + taskId)
                .eventType(NoticeOutboxMessageMapper.EVENT_TYPE)
                .attemptCount(attemptCount)
                .payload(Map.of("taskId", taskId))
                .build();
    }
}
