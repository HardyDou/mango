package io.mango.payment.starter;

import io.mango.payment.core.service.PaymentObservabilityProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mango.payment")
public class PaymentProperties {

    private NotificationProperties notification = new NotificationProperties();

    private PaymentObservabilityProperties observability = new PaymentObservabilityProperties();

    @Data
    public static class NotificationProperties {

        private NotificationDispatchProperties dispatch = new NotificationDispatchProperties();
    }

    @Data
    public static class NotificationDispatchProperties {

        private boolean enabled = true;

        private long intervalMillis = 60_000L;

        private long initialDelayMillis = 30_000L;

        private int tenantLimit = 20;

        private int batchSize = 20;
    }
}
