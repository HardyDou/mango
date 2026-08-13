package io.mango.notice.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mango.notice")
public class NoticeProperties {

    private Outbox outbox = new Outbox();

    private Inbound inbound = new Inbound();

    @Data
    public static class Outbox {

        private boolean enabled = true;

        private boolean dispatchEnabled = true;

        private String workerId = "notice-outbox-worker";

        private int batchSize = 50;

        private int maxAttempts = 3;

        private long retryDelaySeconds = 60L;

        private long initialDelayMillis = 1000L;

        private long fixedDelayMillis = 1000L;
    }

    @Data
    public static class Inbound {

        private boolean enabled = false;

        private int batchSize = 20;

        private int maxAttempts = 5;

        private long pollInitialDelayMillis = 5000L;

        private long pollFixedDelayMillis = 60000L;

        private long workerInitialDelayMillis = 3000L;

        private long workerFixedDelayMillis = 5000L;

        private long lockTtlSeconds = 120L;

        private String workerId = "notice-inbound-worker";
    }
}
