package io.mango.infra.realtime.core.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
public class RealtimeWebSocketConfiguration implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler webSocketHandler;
    private final ProbeWebSocketHandler probeWebSocketHandler;
    private final RealtimeWebSocketHandshakeInterceptor handshakeInterceptor;
    private final RealtimeProbeWebSocketHandshakeInterceptor probeHandshakeInterceptor;
    private final String endpoint;
    private final String probeEndpoint;
    private final List<String> allowedOrigins;

    public RealtimeWebSocketConfiguration(RealtimeWebSocketHandler webSocketHandler,
                                          ProbeWebSocketHandler probeWebSocketHandler,
                                          RealtimeWebSocketHandshakeInterceptor handshakeInterceptor,
                                          RealtimeProbeWebSocketHandshakeInterceptor probeHandshakeInterceptor,
                                          String endpoint,
                                          String probeEndpoint,
                                          List<String> allowedOrigins) {
        this.webSocketHandler = webSocketHandler;
        this.probeWebSocketHandler = probeWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.probeHandshakeInterceptor = probeHandshakeInterceptor;
        this.endpoint = endpoint;
        this.probeEndpoint = probeEndpoint;
        this.allowedOrigins = immutableAllowedOrigins(allowedOrigins);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, endpoint)
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
        registry.addHandler(probeWebSocketHandler, probeEndpoint)
                .addInterceptors(probeHandshakeInterceptor)
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    private List<String> immutableAllowedOrigins(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of("*");
        }
        return List.copyOf(source);
    }
}
