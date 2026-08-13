package io.mango.notice.starter;

import io.mango.notice.core.entity.NoticeInboundMessageEntity;
import io.mango.notice.core.service.NoticeInboundReceiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Retries event-outbox acceptance for persisted inbound messages. */
@Component
@ConditionalOnProperty(prefix = "mango.notice.inbound", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class NoticeInboundBroadcastWorker {

    private final NoticeInboundReceiverService receiverService;
    private final NoticeProperties properties;

    @Scheduled(
            initialDelayString = "${mango.notice.inbound.worker-initial-delay-millis:3000}",
            fixedDelayString = "${mango.notice.inbound.worker-fixed-delay-millis:5000}")
    public void retry() {
        NoticeProperties.Inbound inbound = properties.getInbound();
        for (NoticeInboundMessageEntity message : receiverService.dueBroadcasts(inbound.getBatchSize())) {
            if (message.getAttemptCount() != null && message.getAttemptCount() >= inbound.getMaxAttempts()) {
                receiverService.deadLetterBroadcast(
                        message.getTenantId(), message.getId(), "入站广播重试次数已耗尽");
                continue;
            }
            try {
                receiverService.retryBroadcast(message.getTenantId(), message.getId());
            } catch (RuntimeException failure) {
                log.warn("Inbound broadcast retry failed: tenantId={}, messageId={}, eventId={}",
                        message.getTenantId(), message.getId(), message.getEventId(), failure);
            }
        }
    }
}
