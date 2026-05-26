package io.mango.notice.starter;

import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.starter.OutboxAutoConfiguration;
import io.mango.notice.core.outbox.NoticeOutboxDispatcher;
import io.mango.notice.core.service.INoticeService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@AutoConfiguration(after = NoticeAutoConfiguration.class)
@AutoConfigureAfter(OutboxAutoConfiguration.class)
@EnableConfigurationProperties(NoticeProperties.class)
public class NoticeOutboxAutoConfiguration {

    @Bean
    @ConditionalOnBean(IOutboxStore.class)
    @ConditionalOnProperty(prefix = "mango.notice.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IOutboxDispatcher noticeOutboxDispatcher(IOutboxStore outboxStore,
                                                    INoticeService noticeService,
                                                    NoticeProperties properties) {
        NoticeProperties.Outbox outbox = properties.getOutbox();
        return new NoticeOutboxDispatcher(
                outboxStore,
                noticeService,
                Clock.systemUTC(),
                outbox.getWorkerId(),
                outbox.getBatchSize(),
                outbox.getRetryDelaySeconds(),
                outbox.getMaxAttempts());
    }

    @Bean
    @ConditionalOnBean(IOutboxDispatcher.class)
    @ConditionalOnProperty(prefix = "mango.notice.outbox", name = "dispatch-enabled", havingValue = "true", matchIfMissing = true)
    public NoticeOutboxWorker noticeOutboxWorker(IOutboxDispatcher noticeOutboxDispatcher,
                                                 NoticeProperties properties) {
        NoticeProperties.Outbox outbox = properties.getOutbox();
        return new NoticeOutboxWorker(
                noticeOutboxDispatcher,
                outbox.getWorkerId(),
                outbox.getInitialDelayMillis(),
                outbox.getFixedDelayMillis());
    }
}
