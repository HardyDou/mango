package io.mango.infra.realtime.core.negotiate;

import io.mango.infra.realtime.api.RealtimeHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RealtimeNegotiationController {

    private static final List<String> DEFAULT_PREFERENCE = List.of("websocket", "sse", "polling");

    private final List<RealtimeTransportCapability> transports;

    @GetMapping("${mango.infra.realtime.negotiate.endpoint:/realtime/negotiate}")
    public RealtimeNegotiationResponse negotiate(
            @RequestHeader(value = RealtimeHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(value = "prefer", required = false) String prefer) {

        // Full token validation belongs to gateway/security; realtime only rejects malformed protocol entry.
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("Realtime negotiation rejected: missing or invalid Authorization header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        List<String> preference = parsePreference(prefer);
        return new RealtimeNegotiationResponse(recommendedTransport(preference), transports);
    }

    private List<String> parsePreference(String prefer) {
        if (prefer == null || prefer.isBlank()) {
            return DEFAULT_PREFERENCE;
        }
        List<String> parsed = Arrays.stream(prefer.split(","))
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .filter(item -> !item.isBlank())
                .toList();
        return parsed.isEmpty() ? DEFAULT_PREFERENCE : parsed;
    }

    private String recommendedTransport(List<String> preference) {
        List<String> enabledTypes = new ArrayList<>();
        for (RealtimeTransportCapability transport : transports) {
            if (transport.enabled()) {
                enabledTypes.add(transport.type());
            }
        }
        for (String type : preference) {
            if (enabledTypes.contains(type)) {
                return type;
            }
        }
        return enabledTypes.isEmpty() ? null : enabledTypes.get(0);
    }
}
