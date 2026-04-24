package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.api.RealtimeOutboundApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.IRealtimePublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RealtimeOutboundController implements RealtimeOutboundApi {

    private final IRealtimePublishService realtimePublishService;

    @Override
    @PostMapping("${mango.infra.realtime.outbound.endpoint:/_realtime/messages/outbound}")
    public void dispatch(@RequestBody RealtimeOutboundMessage message) {
        realtimePublishService.publishLocal(message);
    }
}
