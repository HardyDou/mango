package io.mango.notice.starter;

import io.mango.infra.kv.api.IOutboxDispatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Runtime-only Notice outbox dispatch worker. */
@AutoConfiguration(after = NoticeOutboxAutoConfiguration.class)
@ConditionalOnBean({IOutboxDispatcher.class, NoticeProperties.class})
@ConditionalOnProperty(prefix = "mango.bootstrap", name = "mode", havingValue = "runtime",
        matchIfMissing = true)
public class NoticeOutboxRuntimeAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "mango.notice.outbox", name = "dispatch-enabled",
            havingValue = "true", matchIfMissing = true)
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
