package io.mango.infra.realtime.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "mango.infra.realtime")
public class MangoRealtimeProperties {

    /**
     * Master switch for the realtime infrastructure.
     */
    private boolean enabled = true;

    /**
     * Protocol selection mode.
     */
    private RealtimeMode mode = RealtimeMode.AUTO;

    /**
     * SSE protocol settings.
     */
    private Sse sse = new Sse();

    /**
     * WebSocket protocol settings.
     */
    private WebSocket websocket = new WebSocket();

    /**
     * HTTP polling protocol settings.
     */
    private Polling polling = new Polling();

    /**
     * Transport negotiation endpoint settings.
     */
    private Negotiate negotiate = new Negotiate();

    /**
     * Internal remote publishing settings.
     */
    private Remote remote = new Remote();

    /**
     * Client-to-server inbound message settings.
     */
    private Inbound inbound = new Inbound();

    public boolean isSseEffectiveEnabled() {
        if (mode == RealtimeMode.SSE) {
            return true;
        }
        return mode == RealtimeMode.AUTO && sse.isEnabled();
    }

    public boolean isWebsocketEffectiveEnabled() {
        if (mode == RealtimeMode.WEBSOCKET) {
            return true;
        }
        return mode == RealtimeMode.AUTO && websocket.isEnabled();
    }

    public boolean isPollingEffectiveEnabled() {
        if (mode == RealtimeMode.POLLING) {
            return true;
        }
        return mode == RealtimeMode.AUTO && polling.isEnabled();
    }

    public boolean isPublishEffectiveEnabled() {
        return isSseEffectiveEnabled() || isWebsocketEffectiveEnabled() || isPollingEffectiveEnabled();
    }

    public boolean isRemoteEndpointEffectiveEnabled() {
        return isPublishEffectiveEnabled() && remote.isEndpointEnabled();
    }

    @Data
    public static class Sse {

        /**
         * Enables the SSE protocol adapter and endpoint.
         */
        private boolean enabled = true;

        /**
         * SSE subscription endpoint.
         */
        private String endpoint = "/realtime/subscribe";

        /**
         * SSE connection timeout in milliseconds.
         */
        private long timeoutMillis = 5 * 60 * 1000L;
    }

    @Data
    public static class WebSocket {

        /**
         * Enables the WebSocket protocol adapter and endpoint.
         */
        private boolean enabled = true;

        /**
         * WebSocket endpoint.
         */
        private String endpoint = "/realtime/ws";

        /**
         * Allowed origins for the WebSocket endpoint.
         */
        private List<String> allowedOrigins = List.of("*");

        public List<String> getAllowedOrigins() {
            if (allowedOrigins == null || allowedOrigins.isEmpty()) {
                return List.of("*");
            }
            return allowedOrigins;
        }

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/realtime/ws";
            }
            return endpoint;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            if (allowedOrigins == null || allowedOrigins.isEmpty()) {
                this.allowedOrigins = List.of("*");
                return;
            }
            this.allowedOrigins = List.copyOf(allowedOrigins);
        }
    }

    @Data
    public static class Polling {

        /**
         * Enables the HTTP polling protocol adapter and endpoint.
         */
        private boolean enabled = true;

        /**
         * HTTP polling endpoint.
         */
        private String endpoint = "/realtime/poll";

        /**
         * Default max messages returned when caller passes maxSize <= 0.
         */
        private int defaultMaxSize = 20;

        /**
         * Maximum messages returned by one polling request.
         */
        private int maxSize = 100;

        /**
         * Default hold timeout in milliseconds. Zero means short polling.
         */
        private long defaultTimeoutMillis = 0L;

        /**
         * Maximum hold timeout in milliseconds for long polling.
         */
        private long maxTimeoutMillis = 25 * 1000L;

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/realtime/poll";
            }
            return endpoint;
        }

        public int getDefaultMaxSize() {
            if (defaultMaxSize <= 0) {
                return 20;
            }
            return Math.min(defaultMaxSize, getMaxSize());
        }

        public int getMaxSize() {
            return maxSize <= 0 ? 100 : maxSize;
        }

        public long getDefaultTimeoutMillis() {
            if (defaultTimeoutMillis < 0) {
                return 0L;
            }
            return Math.min(defaultTimeoutMillis, getMaxTimeoutMillis());
        }

        public long getMaxTimeoutMillis() {
            return maxTimeoutMillis < 0 ? 0L : maxTimeoutMillis;
        }
    }

    @Data
    public static class Negotiate {

        /**
         * Enables the transport negotiation endpoint.
         */
        private boolean enabled = true;

        /**
         * Transport negotiation endpoint.
         */
        private String endpoint = "/realtime/negotiate";

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/realtime/negotiate";
            }
            return endpoint;
        }
    }

    @Data
    public static class Remote {

        /**
         * Enables the internal /internal/realtime/publish endpoint for remote starter calls.
         */
        private boolean endpointEnabled = true;
    }

    @Data
    public static class Inbound {

        /**
         * Enables WebSocket client-to-server business event dispatch.
         */
        private boolean enabled;

        /**
         * Inbound dispatch mode.
         */
        private RealtimeInboundMode mode = RealtimeInboundMode.NONE;

        /**
         * Maximum accepted WebSocket text payload size in bytes.
         */
        private int maxPayloadBytes = 64 * 1024;

        /**
         * Whether one listener failure should stop subsequent listeners.
         */
        private boolean failFast;

        /**
         * Unknown inbound type policy: ignore, warn or error.
         */
        private String unknownTypePolicy = "ignore";

        /**
         * Remote inbound receiver settings.
         */
        private InboundRemote remote = new InboundRemote();

        public int getMaxPayloadBytes() {
            return maxPayloadBytes <= 0 ? 64 * 1024 : maxPayloadBytes;
        }

        public String getUnknownTypePolicy() {
            if (unknownTypePolicy == null || unknownTypePolicy.isBlank()) {
                return "ignore";
            }
            return unknownTypePolicy;
        }
    }

    @Data
    public static class InboundRemote {

        /**
         * Enables the internal remote inbound endpoint.
         */
        private boolean endpointEnabled = true;

        /**
         * Internal endpoint receiving inbound messages from the realtime service.
         */
        private String endpoint = "/internal/realtime/inbound";

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/internal/realtime/inbound";
            }
            return endpoint;
        }
    }
}
