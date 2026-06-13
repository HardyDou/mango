package io.mango.payment.starter;

import io.mango.payment.core.service.PaymentObservabilityProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mango.payment")
public class PaymentProperties {

    private NotificationProperties notification = new NotificationProperties();

    private PaymentObservabilityProperties observability = new PaymentObservabilityProperties();

    private WorkflowProperties workflow = new WorkflowProperties();

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

    @Data
    public static class WorkflowProperties {

        private RefundApprovalProperties refundApproval = new RefundApprovalProperties();
    }

    @Data
    public static class RefundApprovalProperties {

        private InitializerProperties initializer = new InitializerProperties();
    }

    @Data
    public static class InitializerProperties {

        private boolean enabled;

        private Long systemTenantId;

        private Long systemUserId;

        private String principalName;

        private String realm = "INTERNAL";

        private String actorType = "INTERNAL_USER";

        private String partyType = "INTERNAL_ORG";

        private Long partyId;

        private String appCode;
    }
}
