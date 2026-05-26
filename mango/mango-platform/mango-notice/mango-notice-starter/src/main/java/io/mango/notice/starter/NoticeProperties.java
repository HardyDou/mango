package io.mango.notice.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mango.notice")
public class NoticeProperties {

    private Outbox outbox = new Outbox();

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
}
