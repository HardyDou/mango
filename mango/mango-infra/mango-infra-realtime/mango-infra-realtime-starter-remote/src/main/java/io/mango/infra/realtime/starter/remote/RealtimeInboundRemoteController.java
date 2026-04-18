package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.RealtimeInboundMessage;
import io.mango.infra.realtime.core.inbound.RealtimeInboundDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RealtimeInboundRemoteController {

    private final RealtimeInboundDispatcher dispatcher;

    @PostMapping("${mango.infra.realtime.inbound.remote.endpoint:/internal/realtime/inbound}")
    public void receive(@RequestBody RealtimeInboundMessage message) {
        dispatcher.dispatch(message);
    }
}
