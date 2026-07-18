package io.mango.infra.realtime.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.contract.NativeHttpAdapter;
import io.mango.infra.realtime.api.dto.RealtimeHeaders;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicket;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicketService;
import io.mango.infra.realtime.core.negotiate.RealtimeNegotiationResponse;
import io.mango.infra.realtime.core.negotiate.RealtimeTransportCapability;
import io.mango.infra.realtime.core.web.RealtimeRequestIdentityResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Validated
@NativeHttpAdapter
@RestController
@Tag(name = "实时传输协商", description = "实时通信传输能力协商接口")
public class RealtimeNegotiationController {

    private static final List<String> DEFAULT_PREFERENCE = List.of("websocket", "sse", "polling");

    private final List<RealtimeTransportCapability> transports;
    private final RealtimeConnectionTicketService ticketService;

    public RealtimeNegotiationController(List<RealtimeTransportCapability> transports,
                                         RealtimeConnectionTicketService ticketService) {
        this.transports = immutableTransports(transports);
        this.ticketService = ticketService;
    }

    @GetMapping("${mango.infra.realtime.negotiate.endpoint:/realtime/transports/negotiate}")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "协商实时传输协议")
    @Operation(summary = "协商实时传输协议", description = "登录接口。根据客户端偏好协商实时消息传输协议")
    public RealtimeNegotiationResponse negotiate(
            @Parameter(description = "客户端偏好的传输协议，多个值用英文逗号分隔，例如 websocket,sse,polling")
            @RequestParam(value = "prefer", required = false) String prefer,
            @Parameter(description = "客户端是否支持 WebSocket")
            @RequestParam(value = "clientWebSocket", required = false, defaultValue = "true") boolean clientWebSocket,
            @Parameter(description = "客户端是否支持 SSE")
            @RequestParam(value = "clientSse", required = false, defaultValue = "true") boolean clientSse,
            @Parameter(description = "客户端是否支持长轮询")
            @RequestParam(value = "clientPolling", required = false, defaultValue = "true") boolean clientPolling,
            @Parameter(description = "WebSocket 是否可携带认证票据")
            @RequestParam(value = "wsTokenAvailable", required = false, defaultValue = "false") boolean wsTokenAvailable,
            @Parameter(description = "SSE 是否可携带认证票据")
            @RequestParam(value = "sseTokenAvailable", required = false, defaultValue = "false") boolean sseTokenAvailable,
            @Parameter(description = "客户端是否允许使用 Cookie")
            @RequestParam(value = "cookieAvailable", required = false, defaultValue = "false") boolean cookieAvailable,
            @Parameter(description = "当前页面协议，例如 https")
            @RequestParam(value = "pageProtocol", required = false) String pageProtocol,
            HttpServletRequest request) {
        List<String> preference = parsePreference(prefer);
        boolean requestHasCookie = hasText(request.getHeader("Cookie"));
        List<RealtimeTransportCapability> negotiated = negotiatedTransports(
                clientWebSocket,
                clientSse,
                clientPolling,
                wsTokenAvailable,
                sseTokenAvailable,
                cookieAvailable && requestHasCookie,
                pageProtocol,
                request);
        List<String> order = orderedTransports(preference, negotiated);
        String recommended = firstOrNull(order);
        RealtimeConnectionTicket ticket = ticketService.issue(
                RealtimeRequestIdentityResolver.resolveTenantId(
                        request,
                        request.getHeader(RealtimeHeaders.TENANT_ID)),
                RealtimeRequestIdentityResolver.resolveUserId(
                        request,
                        request.getHeader(RealtimeHeaders.USER_ID)),
                firstText(request.getHeader(RealtimeHeaders.CLIENT_ID), "browser"),
                profile(request));
        return new RealtimeNegotiationResponse(recommended, negotiated, order, ticket.value(), ticket.expiresAt());
    }

    private List<String> parsePreference(String prefer) {
        if (prefer == null || prefer.isBlank()) {
            return DEFAULT_PREFERENCE;
        }
        List<String> parsed = Arrays.stream(prefer.split(","))
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .filter(item -> !item.isBlank())
                .toList();
        if (parsed.isEmpty()) {
            return DEFAULT_PREFERENCE;
        }
        return parsed;
    }

    private List<RealtimeTransportCapability> negotiatedTransports(
            boolean clientWebSocket,
            boolean clientSse,
            boolean clientPolling,
            boolean wsTokenAvailable,
            boolean sseTokenAvailable,
            boolean cookieAvailable,
            String pageProtocol,
            HttpServletRequest request) {
        List<RealtimeTransportCapability> result = new ArrayList<>();
        for (RealtimeTransportCapability transport : transports) {
            boolean serverEnabled = transport.enabled();
            boolean clientSupported = clientSupports(transport.type(), clientWebSocket, clientSse, clientPolling);
            boolean contextReady = contextReady(transport.type(), wsTokenAvailable, sseTokenAvailable, cookieAvailable, request);
            boolean available = serverEnabled && clientSupported && contextReady;
            result.add(new RealtimeTransportCapability(
                    transport.type(),
                    transport.enabled(),
                    transport.endpoint(),
                    transport.bidirectional(),
                    transport.longPolling(),
                    transport.defaultMaxSize(),
                    transport.maxSize(),
                    transport.defaultTimeoutMillis(),
                    transport.maxTimeoutMillis(),
                    available,
                    unavailableReason(transport, serverEnabled, clientSupported, contextReady, clientWebSocket, clientSse, clientPolling, pageProtocol),
                    serverEnabled,
                    clientSupported,
                    contextReady,
                    handshakeRequired(transport.type()),
                    true,
                    probeEndpoint(transport.type())));
        }
        return result;
    }

    private List<String> orderedTransports(List<String> preference, List<RealtimeTransportCapability> negotiated) {
        List<String> enabledTypes = new ArrayList<>();
        for (RealtimeTransportCapability transport : negotiated) {
            if (transport.enabled() && transport.available()) {
                enabledTypes.add(transport.type());
            }
        }
        List<String> order = new ArrayList<>();
        for (String type : preference) {
            if (enabledTypes.contains(type)) {
                order.add(type);
            }
        }
        for (String type : enabledTypes) {
            if (!order.contains(type)) {
                order.add(type);
            }
        }
        return order;
    }

    private boolean clientSupports(String type, boolean clientWebSocket, boolean clientSse, boolean clientPolling) {
        return switch (type) {
            case "websocket" -> clientWebSocket;
            case "sse" -> clientSse;
            case "polling" -> clientPolling;
            default -> false;
        };
    }

    private String unavailableReason(
            RealtimeTransportCapability transport,
            boolean serverEnabled,
            boolean clientSupported,
            boolean contextReady,
            boolean clientWebSocket,
            boolean clientSse,
            boolean clientPolling,
            String pageProtocol) {
        if (serverEnabled && clientSupported && contextReady) {
            return "";
        }
        if (!serverEnabled) {
            return "server_disabled";
        }
        String clientReason = clientUnavailableReason(
                transport.type(), clientWebSocket, clientSse, clientPolling);
        if (!clientReason.isEmpty()) {
            return clientReason;
        }
        if (!contextReady) {
            return "context_not_ready";
        }
        return unavailableForPageProtocol(pageProtocol);
    }

    private boolean contextReady(
            String type,
            boolean wsTokenAvailable,
            boolean sseTokenAvailable,
            boolean cookieAvailable,
            HttpServletRequest request) {
        if ("polling".equals(type)) {
            return hasText(request.getHeader("Authorization"));
        }
        if ("websocket".equals(type)) {
            return hasText(request.getHeader("Authorization")) || wsTokenAvailable || cookieAvailable;
        }
        if ("sse".equals(type)) {
            return hasText(request.getHeader("Authorization")) || sseTokenAvailable || cookieAvailable;
        }
        return false;
    }

    private boolean handshakeRequired(String type) {
        return "websocket".equals(type) || "sse".equals(type);
    }

    private String probeEndpoint(String type) {
        return switch (type) {
            case "websocket" -> "/realtime/transports/probe/websocket";
            case "sse" -> "/realtime/transports/probe/sse";
            case "polling" -> "/realtime/transports/probe/polling";
            default -> "";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> profile(HttpServletRequest request) {
        Map<String, Object> profile = new LinkedHashMap<>();
        putIfText(profile, "clientId", request.getHeader(RealtimeHeaders.CLIENT_ID));
        return profile;
    }

    private void putIfText(Map<String, Object> target, String key, String value) {
        if (hasText(value)) {
            target.put(key, value);
        }
    }

    private String firstOrNull(List<String> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private String clientUnavailableReason(String type,
                                           boolean clientWebSocket,
                                           boolean clientSse,
                                           boolean clientPolling) {
        return switch (type) {
            case "websocket" -> unavailableIfUnsupported(clientWebSocket, "client_websocket_unavailable");
            case "sse" -> unavailableIfUnsupported(clientSse, "client_sse_unavailable");
            case "polling" -> unavailableIfUnsupported(clientPolling, "client_polling_unavailable");
            default -> "";
        };
    }

    private String unavailableIfUnsupported(boolean supported, String reason) {
        if (supported) {
            return "";
        }
        return reason;
    }

    private String unavailableForPageProtocol(String pageProtocol) {
        if (pageProtocol == null || pageProtocol.isBlank()) {
            return "unavailable";
        }
        return "unavailable_on_" + pageProtocol;
    }

    private List<RealtimeTransportCapability> immutableTransports(List<RealtimeTransportCapability> source) {
        if (source == null) {
            return List.of();
        }
        return List.copyOf(source);
    }
}
