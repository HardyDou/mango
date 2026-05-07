package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.core.negotiate.RealtimeNegotiationResponse;
import io.mango.infra.realtime.core.negotiate.RealtimeTransportCapability;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@Tag(name = "实时传输协商", description = "实时通信传输能力协商接口")
public class RealtimeNegotiationController {

    private static final List<String> DEFAULT_PREFERENCE = List.of("websocket", "sse", "polling");

    private final List<RealtimeTransportCapability> transports;

    @GetMapping("${mango.infra.realtime.negotiate.endpoint:/realtime/transports/negotiate}")
    public RealtimeNegotiationResponse negotiate(@RequestParam(value = "prefer", required = false) String prefer) {
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
