package io.mango.infra.realtime.core.inbound.forward;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;

import java.util.List;

public final class RealtimeInboundForwardServices {

    private static final IRealtimeInboundForwardService NOOP = message -> {
        // Inbound dispatch is disabled.
    };

    private RealtimeInboundForwardServices() {
    }

    public static IRealtimeInboundForwardService noop() {
        return NOOP;
    }

    public static IRealtimeInboundForwardService local(IRealtimeInboundService realtimeInboundService) {
        return realtimeInboundService::dispatch;
    }

    public static IRealtimeInboundForwardService composite(List<IRealtimeInboundForwardService> forwardServices) {
        List<IRealtimeInboundForwardService> delegates = activeForwardServices(forwardServices);
        if (delegates.isEmpty()) {
            return noop();
        }
        if (delegates.size() == 1) {
            return delegates.get(0);
        }
        return message -> forwardAll(delegates, message);
    }

    private static boolean isActiveForwardService(IRealtimeInboundForwardService forwardService) {
        return forwardService != null && forwardService != NOOP;
    }

    private static List<IRealtimeInboundForwardService> activeForwardServices(
            List<IRealtimeInboundForwardService> forwardServices) {
        if (forwardServices == null) {
            return List.of();
        }
        return forwardServices.stream()
                .filter(RealtimeInboundForwardServices::isActiveForwardService)
                .toList();
    }

    private static void forwardAll(List<IRealtimeInboundForwardService> forwardServices, RealtimeInboundMessage message) {
        for (IRealtimeInboundForwardService forwardService : forwardServices) {
            forwardService.forward(message);
        }
    }
}
