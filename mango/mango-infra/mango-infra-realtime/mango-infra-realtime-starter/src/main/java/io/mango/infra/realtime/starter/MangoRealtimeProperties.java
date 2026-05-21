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
     * Current realtime node route identity.
     */
    private Node node = new Node();

    /**
     * Cross-node outbound message forwarding settings.
     */
    private Outbound outbound = new Outbound();

    /**
     * Online presence route settings.
     */
    private Presence presence = new Presence();

    /**
     * Reliable realtime dispatch settings backed by infra-kv outbox.
     */
    private Outbox outbox = new Outbox();

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
        private String endpoint = "/realtime/transports/sse";

        /**
         * SSE connection timeout in milliseconds.
         */
        private long timeoutMillis = 5 * 60 * 1000L;

        /**
         * HTTP inbound endpoint used by SSE clients to send messages upstream.
         */
        private String inboundEndpoint = "/realtime/messages/inbound/sse";

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/realtime/transports/sse";
            }
            return endpoint;
        }

        public String getInboundEndpoint() {
            if (inboundEndpoint == null || inboundEndpoint.isBlank()) {
                return "/realtime/messages/inbound/sse";
            }
            return inboundEndpoint;
        }
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
        private String endpoint = "/realtime/transports/websocket";

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
                return "/realtime/transports/websocket";
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
        private String endpoint = "/realtime/transports/polling";

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

        /**
         * HTTP inbound endpoint used by polling clients to send messages upstream.
         */
        private String inboundEndpoint = "/realtime/messages/inbound/polling";

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/realtime/transports/polling";
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

        public String getInboundEndpoint() {
            if (inboundEndpoint == null || inboundEndpoint.isBlank()) {
                return "/realtime/messages/inbound/polling";
            }
            return inboundEndpoint;
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
        private String endpoint = "/realtime/transports/negotiate";

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/realtime/transports/negotiate";
            }
            return endpoint;
        }
    }

    @Data
    public static class Remote {

        /**
         * Enables the forward /realtime/messages/publish endpoint for remote starter calls.
         */
        private boolean endpointEnabled = true;
    }

    @Data
    public static class Node {

        /**
         * Stable instance id used to distinguish nodes of the same service.
         */
        private String instanceId;

        /**
         * Routable service name for reverse calls. Defaults to spring.application.name.
         */
        private String serviceName;

        /**
         * Runtime servlet context path. Defaults to server.servlet.context-path or /.
         */
        private String contextPath;
    }

    @Data
    public static class Outbound {

        /**
         * Enables the reverse outbound endpoint for cross-node server-to-client dispatch.
         */
        private boolean endpointEnabled = true;

        /**
         * Reverse endpoint receiving server-to-client messages from peer realtime nodes.
         */
        private String endpoint = "/_realtime/messages/outbound";

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/_realtime/messages/outbound";
            }
            return endpoint;
        }
    }

    @Data
    public static class Presence {

        /**
         * KV key prefix for multi-instance online presence routes.
         */
        private String prefix = "mango:infra:realtime:presence";

        /**
         * Presence TTL in seconds. Nodes refresh local sessions periodically before expiry.
         */
        private long ttlSeconds = 120L;

        public String getPrefix() {
            if (prefix == null || prefix.isBlank()) {
                return "mango:infra:realtime:presence";
            }
            return prefix;
        }

        public long getTtlSeconds() {
            return ttlSeconds <= 0 ? 120L : ttlSeconds;
        }
    }

    @Data
    public static class Outbox {

        /**
         * Enables reliable realtime publishing through infra-kv outbox.
         */
        private boolean enabled = true;

        /**
         * Worker id used when claiming outbox messages.
         */
        private String workerId;

        /**
         * Claim batch size.
         */
        private int batchSize = 50;

        /**
         * Initial dispatcher delay in milliseconds.
         */
        private long initialDelayMillis = 1000L;

        /**
         * Fixed dispatcher delay in milliseconds.
         */
        private long fixedDelayMillis = 500L;

        /**
         * Maximum dispatch attempts before continuing delayed retries.
         */
        private int maxAttempts = 5;

        /**
         * Base retry backoff in milliseconds.
         */
        private long retryBackoffMillis = 1000L;

        public int getBatchSize() {
            return batchSize <= 0 ? 50 : batchSize;
        }

        public long getInitialDelayMillis() {
            return Math.max(0L, initialDelayMillis);
        }

        public long getFixedDelayMillis() {
            return fixedDelayMillis <= 0 ? 500L : fixedDelayMillis;
        }

        public int getMaxAttempts() {
            return maxAttempts <= 0 ? 5 : maxAttempts;
        }

        public long getRetryBackoffMillis() {
            return retryBackoffMillis <= 0 ? 1000L : retryBackoffMillis;
        }
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
        private RealtimeInboundMode mode = RealtimeInboundMode.LOCAL_REMOTE;

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
         * Enables the reverse remote inbound endpoint.
         */
        private boolean endpointEnabled = true;

        /**
         * Reverse endpoint receiving inbound messages from the realtime service.
         */
        private String endpoint = "/_realtime/messages/inbound";

        public String getEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                return "/_realtime/messages/inbound";
            }
            return endpoint;
        }
    }
}
