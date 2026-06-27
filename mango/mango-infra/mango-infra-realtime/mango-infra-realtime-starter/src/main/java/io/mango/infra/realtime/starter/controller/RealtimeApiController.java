package io.mango.infra.realtime.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.IRealtimeReliablePublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@ApiAccess(mode = ApiResourceAccessMode.INTERNAL)
@Tag(name = "实时消息", description = "实时消息发布接口")
public class RealtimeApiController implements RealtimeApi {

    private final IRealtimeReliablePublishService realtimePublishService;

    @Override
    @PostMapping("/publish")
    @Operation(summary = "发布实时消息", description = "内部接口。发布实时下行消息到实时通道")
    public void publish(@RequestBody RealtimeOutboundMessage message) {
        realtimePublishService.publish(message);
    }
}
