package io.mango.infra.realtime.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "mango.infra.realtime")
public class MangoRealtimeProperties {

    private static final long DEFAULT_SSE_TIMEOUT_MILLIS = 300_000L;
    private static final int DEFAULT_POLLING_MAX_SIZE = 20;
    private static final int MAX_POLLING_MAX_SIZE = 100;
    private static final long MAX_POLLING_TIMEOUT_MILLIS = 25_000L;
    private static final long DEFAULT_PRESENCE_TTL_SECONDS = 120L;
    private static final int DEFAULT_OUTBOX_BATCH_SIZE = 50;
    private static final long DEFAULT_OUTBOX_INITIAL_DELAY_MILLIS = 1_000L;
    private static final long DEFAULT_OUTBOX_FIXED_DELAY_MILLIS = 500L;
    private static final int DEFAULT_OUTBOX_MAX_ATTEMPTS = 5;
    private static final long DEFAULT_OUTBOX_RETRY_BACKOFF_MILLIS = 1_000L;
    private static final int DEFAULT_INBOUND_MAX_PAYLOAD_BYTES = 65_536;

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
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private Sse sse = new Sse();

    /**
     * WebSocket protocol settings.
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private WebSocket websocket = new WebSocket();

    /**
     * HTTP polling protocol settings.
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private Polling polling = new Polling();

    /**
     * Transport negotiation endpoint settings.
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private Negotiate negotiate = new Negotiate();

    /**
     * Internal remote publishing settings.
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private Remote remote = new Remote();

    /**
     * Current realtime node route identity.
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private Node node = new Node();

    /**
     * Cross-node outbound message forwarding settings.
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private Outbound outbound = new Outbound();

    /**
     * Online presence route settings.
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private Presence presence = new Presence();

    /**
     * Reliable realtime dispatch settings backed by infra-kv outbox.
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    private Outbox outbox = new Outbox();

    /**
     * Client-to-server inbound message settings.
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding requires a mutable nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding requires a mutable nested property bean"))
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

    @Getter
    @Setter
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
        private long timeoutMillis = DEFAULT_SSE_TIMEOUT_MILLIS;

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

    @Getter
    @Setter
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

    @Getter
    @Setter
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
        private int defaultMaxSize = DEFAULT_POLLING_MAX_SIZE;

        /**
         * Maximum messages returned by one polling request.
         */
        private int maxSize = MAX_POLLING_MAX_SIZE;

        /**
         * Default hold timeout in milliseconds. Zero means short polling.
         */
        private long defaultTimeoutMillis = 0L;

        /**
         * Maximum hold timeout in milliseconds for long polling.
         */
        private long maxTimeoutMillis = MAX_POLLING_TIMEOUT_MILLIS;

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
                return DEFAULT_POLLING_MAX_SIZE;
            }
            return Math.min(defaultMaxSize, getMaxSize());
        }

        public int getMaxSize() {
            if (maxSize <= 0) {
                return MAX_POLLING_MAX_SIZE;
            }
            return maxSize;
        }

        public long getDefaultTimeoutMillis() {
            if (defaultTimeoutMillis < 0) {
                return 0L;
            }
            return Math.min(defaultTimeoutMillis, getMaxTimeoutMillis());
        }

        public long getMaxTimeoutMillis() {
            if (maxTimeoutMillis < 0) {
                return 0L;
            }
            return maxTimeoutMillis;
        }

        public String getInboundEndpoint() {
            if (inboundEndpoint == null || inboundEndpoint.isBlank()) {
                return "/realtime/messages/inbound/polling";
            }
            return inboundEndpoint;
        }
    }

    @Getter
    @Setter
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

    @Getter
    @Setter
    public static class Remote {

        /**
         * Enables the forward /realtime/messages/publish endpoint for remote starter calls.
         */
        private boolean endpointEnabled = true;
    }

    @Getter
    @Setter
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

    @Getter
    @Setter
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

    @Getter
    @Setter
    public static class Presence {

        /**
         * KV key prefix for multi-instance online presence routes.
         */
        private String prefix = "mango:infra:realtime:presence";

        /**
         * Presence TTL in seconds. Nodes refresh local sessions periodically before expiry.
         */
        private long ttlSeconds = DEFAULT_PRESENCE_TTL_SECONDS;

        public String getPrefix() {
            if (prefix == null || prefix.isBlank()) {
                return "mango:infra:realtime:presence";
            }
            return prefix;
        }

        public long getTtlSeconds() {
            if (ttlSeconds <= 0) {
                return DEFAULT_PRESENCE_TTL_SECONDS;
            }
            return ttlSeconds;
        }
    }

    @Getter
    @Setter
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
        private int batchSize = DEFAULT_OUTBOX_BATCH_SIZE;

        /**
         * Initial dispatcher delay in milliseconds.
         */
        private long initialDelayMillis = DEFAULT_OUTBOX_INITIAL_DELAY_MILLIS;

        /**
         * Fixed dispatcher delay in milliseconds.
         */
        private long fixedDelayMillis = DEFAULT_OUTBOX_FIXED_DELAY_MILLIS;

        /**
         * Maximum dispatch attempts before continuing delayed retries.
         */
        private int maxAttempts = DEFAULT_OUTBOX_MAX_ATTEMPTS;

        /**
         * Base retry backoff in milliseconds.
         */
        private long retryBackoffMillis = DEFAULT_OUTBOX_RETRY_BACKOFF_MILLIS;

        public int getBatchSize() {
            if (batchSize <= 0) {
                return DEFAULT_OUTBOX_BATCH_SIZE;
            }
            return batchSize;
        }

        public long getInitialDelayMillis() {
            return Math.max(0L, initialDelayMillis);
        }

        public long getFixedDelayMillis() {
            if (fixedDelayMillis <= 0) {
                return DEFAULT_OUTBOX_FIXED_DELAY_MILLIS;
            }
            return fixedDelayMillis;
        }

        public int getMaxAttempts() {
            if (maxAttempts <= 0) {
                return DEFAULT_OUTBOX_MAX_ATTEMPTS;
            }
            return maxAttempts;
        }

        public long getRetryBackoffMillis() {
            if (retryBackoffMillis <= 0) {
                return DEFAULT_OUTBOX_RETRY_BACKOFF_MILLIS;
            }
            return retryBackoffMillis;
        }
    }

    @Getter
    @Setter
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
        private int maxPayloadBytes = DEFAULT_INBOUND_MAX_PAYLOAD_BYTES;

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
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration binding requires a mutable nested property bean"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration binding requires a mutable nested property bean"))
        private InboundRemote remote = new InboundRemote();

        public int getMaxPayloadBytes() {
            if (maxPayloadBytes <= 0) {
                return DEFAULT_INBOUND_MAX_PAYLOAD_BYTES;
            }
            return maxPayloadBytes;
        }

        public String getUnknownTypePolicy() {
            if (unknownTypePolicy == null || unknownTypePolicy.isBlank()) {
                return "ignore";
            }
            return unknownTypePolicy;
        }
    }

    @Getter
    @Setter
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
