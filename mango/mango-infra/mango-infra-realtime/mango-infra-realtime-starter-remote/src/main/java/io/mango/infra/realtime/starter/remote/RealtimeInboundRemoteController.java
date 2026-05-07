package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.RealtimeInboundApi;
import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "实时入站-远程", description = "远程实时入站消息分发接口")
public class RealtimeInboundRemoteController implements RealtimeInboundApi {

    private final IRealtimeInboundService realtimeInboundService;

    @Override
    @PostMapping("${mango.infra.realtime.inbound.remote.endpoint:/_realtime/messages/inbound}")
    public void dispatch(@RequestBody RealtimeInboundMessage message) {
        realtimeInboundService.dispatch(message);
    }

}
