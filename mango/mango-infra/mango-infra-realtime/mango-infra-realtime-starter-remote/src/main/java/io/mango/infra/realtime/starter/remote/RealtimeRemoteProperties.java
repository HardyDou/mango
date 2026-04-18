package io.mango.infra.realtime.starter.remote;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mango.infra.realtime")
public class RealtimeRemoteProperties {

    private Inbound inbound = new Inbound();

    @Data
    public static class Inbound {

        private boolean enabled;

        private boolean failFast;

        private String unknownTypePolicy = "ignore";

        private Remote remote = new Remote();

        public String getUnknownTypePolicy() {
            if (unknownTypePolicy == null || unknownTypePolicy.isBlank()) {
                return "ignore";
            }
            return unknownTypePolicy;
        }
    }

    @Data
    public static class Remote {

        private boolean endpointEnabled = true;

        private boolean registerEnabled = true;

        private String endpoint = "/internal/realtime/inbound";

        private String serviceName;

        private String contextPath = "/";

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/internal/realtime/inbound";
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
