package io.mango.notice.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mango.notice")
public class NoticeProperties {

    private static final int DEFAULT_OUTBOX_BATCH_SIZE = 50;
    private static final int DEFAULT_OUTBOX_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_OUTBOX_RETRY_DELAY_SECONDS = 60L;
    private static final long DEFAULT_OUTBOX_INITIAL_DELAY_MILLIS = 1000L;
    private static final long DEFAULT_OUTBOX_FIXED_DELAY_MILLIS = 1000L;
    private static final int DEFAULT_INBOUND_BATCH_SIZE = 20;
    private static final int DEFAULT_INBOUND_MAX_ATTEMPTS = 5;
    private static final long DEFAULT_INBOUND_POLL_INITIAL_DELAY_MILLIS = 5000L;
    private static final long DEFAULT_INBOUND_POLL_FIXED_DELAY_MILLIS = 60000L;
    private static final long DEFAULT_INBOUND_WORKER_INITIAL_DELAY_MILLIS = 3000L;
    private static final long DEFAULT_INBOUND_WORKER_FIXED_DELAY_MILLIS = 5000L;
    private static final long DEFAULT_INBOUND_LOCK_TTL_SECONDS = 120L;

    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Nested Spring configuration is exposed for property binding"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Nested Spring configuration is accepted during property binding"))
    private Outbox outbox = new Outbox();

    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Nested Spring configuration is exposed for property binding"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Nested Spring configuration is accepted during property binding"))
    private Inbound inbound = new Inbound();

    @Data
    public static class Outbox {

        private boolean enabled = true;

        private boolean dispatchEnabled = true;

        private String workerId = "notice-outbox-worker";

        private int batchSize = DEFAULT_OUTBOX_BATCH_SIZE;

        private int maxAttempts = DEFAULT_OUTBOX_MAX_ATTEMPTS;

        private long retryDelaySeconds = DEFAULT_OUTBOX_RETRY_DELAY_SECONDS;

        private long initialDelayMillis = DEFAULT_OUTBOX_INITIAL_DELAY_MILLIS;

        private long fixedDelayMillis = DEFAULT_OUTBOX_FIXED_DELAY_MILLIS;
    }

    @Data
    public static class Inbound {

        private boolean enabled = false;

        private int batchSize = DEFAULT_INBOUND_BATCH_SIZE;

        private int maxAttempts = DEFAULT_INBOUND_MAX_ATTEMPTS;

        private long pollInitialDelayMillis = DEFAULT_INBOUND_POLL_INITIAL_DELAY_MILLIS;

        private long pollFixedDelayMillis = DEFAULT_INBOUND_POLL_FIXED_DELAY_MILLIS;

        private long workerInitialDelayMillis = DEFAULT_INBOUND_WORKER_INITIAL_DELAY_MILLIS;

        private long workerFixedDelayMillis = DEFAULT_INBOUND_WORKER_FIXED_DELAY_MILLIS;

        private long lockTtlSeconds = DEFAULT_INBOUND_LOCK_TTL_SECONDS;

        private String workerId = "notice-inbound-worker";
    }
}
