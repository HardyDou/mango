package io.mango.infra.realtime.starter.remote;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mango.infra.realtime")
public class RealtimeRemoteProperties {

    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private Inbound inbound = new Inbound();

    @Getter
    @Setter
    public static class Inbound {

        private boolean enabled;

        private boolean failFast;

        private String unknownTypePolicy = "ignore";

        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration binding requires a mutable nested property bean"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration binding requires a mutable nested property bean"))
        private Remote remote = new Remote();

        public String getUnknownTypePolicy() {
            if (unknownTypePolicy == null || unknownTypePolicy.isBlank()) {
                return "ignore";
            }
            return unknownTypePolicy;
        }
    }

    @Getter
    @Setter
    public static class Remote {

        private boolean endpointEnabled = true;

        private boolean registerEnabled = true;

        private String endpoint = "/_realtime/messages/inbound";

        private String serviceName;

        private String contextPath = "/";

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/_realtime/messages/inbound";
            }
            return endpoint;
        }

        public String getContextPath() {
            if (contextPath == null || contextPath.isBlank()) {
                return "/";
            }
            return contextPath;
        }
    }
}
