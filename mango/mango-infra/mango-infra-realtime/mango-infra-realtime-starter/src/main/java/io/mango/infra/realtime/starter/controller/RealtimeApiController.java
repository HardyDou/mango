package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.IRealtimePublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Realtime publishing endpoint for forward remote calls.
 */
@RestController
@RequestMapping("/realtime/messages")
@RequiredArgsConstructor
public class RealtimeApiController implements RealtimeApi {

    private final IRealtimePublishService realtimePublishService;

    @Override
    @PostMapping("/publish")
    public void publish(@RequestBody RealtimeOutboundMessage message) {
        realtimePublishService.publish(message);
    }
}
