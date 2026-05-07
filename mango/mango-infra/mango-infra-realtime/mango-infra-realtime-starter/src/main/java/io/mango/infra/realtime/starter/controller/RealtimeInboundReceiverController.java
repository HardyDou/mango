package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.api.RealtimeInboundReceiverApi;
import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;
import io.mango.infra.realtime.core.inbound.receiver.IRealtimeInboundReceiverService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receiver-service registry endpoint for forward remote calls.
 */
@RestController
@RequestMapping("/realtime/receivers")
@RequiredArgsConstructor
@Tag(name = "实时接收器", description = "实时消息接收器注册与注销接口")
public class RealtimeInboundReceiverController implements RealtimeInboundReceiverApi {

    private final IRealtimeInboundReceiverService realtimeInboundReceiverService;

    @Override
    @PostMapping("/register")
    public void register(@RequestBody RealtimeInboundReceiverRegistration registration) {
        realtimeInboundReceiverService.register(registration);
    }

    @Override
    @PostMapping("/unregister")
    public void unregister(@RequestBody RealtimeInboundReceiverRegistration registration) {
        realtimeInboundReceiverService.unregister(registration);
    }
}
