package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.api.RealtimeOutboundApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.IRealtimePublishService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "实时出站", description = "实时消息本地出站派发接口")
public class RealtimeOutboundController implements RealtimeOutboundApi {

    private final IRealtimePublishService realtimePublishService;

    @Override
    @PostMapping("${mango.infra.realtime.outbound.endpoint:/_realtime/messages/outbound}")
    public void dispatch(@RequestBody RealtimeOutboundMessage message) {
        realtimePublishService.publishLocal(message);
    }
}
