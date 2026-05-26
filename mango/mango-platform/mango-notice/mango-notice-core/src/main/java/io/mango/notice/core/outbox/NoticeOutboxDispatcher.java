package io.mango.notice.core.outbox;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.notice.core.service.INoticeService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class NoticeOutboxDispatcher implements IOutboxDispatcher {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final IOutboxStore outboxStore;
    private final INoticeService noticeService;
    private final Clock clock;
    private final String workerId;
    private final int batchSize;
    private final long retryDelaySeconds;
    private final int maxAttempts;

    public NoticeOutboxDispatcher(IOutboxStore outboxStore,
                                  INoticeService noticeService,
                                  Clock clock,
                                  String workerId,
                                  int batchSize,
                                  long retryDelaySeconds) {
        this(outboxStore, noticeService, clock, workerId, batchSize, retryDelaySeconds, DEFAULT_MAX_ATTEMPTS);
    }

    public NoticeOutboxDispatcher(IOutboxStore outboxStore,
                                  INoticeService noticeService,
                                  Clock clock,
                                  String workerId,
                                  int batchSize,
                                  long retryDelaySeconds,
                                  int maxAttempts) {
        Require.notNull(outboxStore, "Outbox 存储不能为空");
        Require.notNull(noticeService, "通知服务不能为空");
        this.outboxStore = outboxStore;
        this.noticeService = noticeService;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.workerId = workerId == null || workerId.isBlank() ? "notice-outbox-worker" : workerId.trim();
        this.batchSize = batchSize;
        this.retryDelaySeconds = Math.max(0, retryDelaySeconds);
        this.maxAttempts = maxAttempts <= 0 ? DEFAULT_MAX_ATTEMPTS : maxAttempts;
    }

    @Override
    public int dispatchOnce() {
        if (batchSize <= 0) {
            return 0;
        }
        List<OutboxMessage> messages = outboxStore.claim(
                workerId,
                NoticeOutboxMessageMapper.EVENT_TYPE,
                batchSize,
                clock.instant());
        int handled = 0;
        for (OutboxMessage message : messages) {
            try {
                Long taskId = taskId(message);
                noticeService.executeTask(taskId);
                Instant now = clock.instant();
                if (noticeService.hasRetryWaitingRecords(taskId) && message.getAttemptCount() < maxAttempts) {
                    outboxStore.nack(
                            message.getMessageId(),
                            workerId,
                            "通知任务仍有待重试记录",
                            now.plusSeconds(retryDelaySeconds),
                            now);
                } else {
                    if (noticeService.hasRetryWaitingRecords(taskId)) {
                        noticeService.finalizeRetryWaitingRecords(taskId, "通知任务达到最大重试次数");
                    }
                    outboxStore.ack(message.getMessageId(), workerId, now);
                }
                handled++;
            } catch (RuntimeException ex) {
                Instant now = clock.instant();
                if (message.getAttemptCount() >= maxAttempts) {
                    Long parsedTaskId = taskIdOrNull(message);
                    if (parsedTaskId != null) {
                        noticeService.finalizeRetryWaitingRecords(parsedTaskId, ex.getMessage());
                    }
                    outboxStore.ack(message.getMessageId(), workerId, now);
                    handled++;
                } else {
                    outboxStore.nack(
                            message.getMessageId(),
                            workerId,
                            ex.getMessage(),
                            now.plusSeconds(retryDelaySeconds),
                            now);
                }
            }
        }
        return handled;
    }

    private Long taskId(OutboxMessage message) {
        Object taskId = message.getPayload().get("taskId");
        if (taskId instanceof Number number) {
            return number.longValue();
        }
        if (taskId instanceof String value && !value.isBlank()) {
            return Long.valueOf(value);
        }
        throw new IllegalArgumentException("通知 outbox 缺少 taskId");
    }

    private Long taskIdOrNull(OutboxMessage message) {
        Object taskId = message.getPayload().get("taskId");
        if (taskId instanceof Number number) {
            return number.longValue();
        }
        if (taskId instanceof String value && !value.isBlank()) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }
}
