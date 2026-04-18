package io.mango.infra.realtime.core.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RealtimeWebSocketConfiguration implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler webSocketHandler;
    private final RealtimeWebSocketHandshakeInterceptor handshakeInterceptor;
    private final String endpoint;
    private final List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, endpoint)
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}
