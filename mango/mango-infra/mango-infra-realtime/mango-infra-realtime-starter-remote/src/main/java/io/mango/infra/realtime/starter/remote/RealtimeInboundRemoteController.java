package io.mango.infra.realtime.starter.remote;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.contract.NativeHttpAdapter;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@NativeHttpAdapter
@RestController
@RequiredArgsConstructor
@ApiAccess(mode = ApiResourceAccessMode.INTERNAL)
@Tag(name = "实时入站-远程", description = "远程实时入站消息分发接口")
public class RealtimeInboundRemoteController {

    private final IRealtimeInboundService realtimeInboundService;

    @PostMapping("${mango.infra.realtime.inbound.remote.endpoint:/_realtime/messages/inbound}")
    @Operation(summary = "分发远程实时入站消息", description = "内部接口。向当前服务的实时消息监听器分发入站消息")
    public void dispatch(@Valid @RequestBody RealtimeInboundMessage message) {
        realtimeInboundService.dispatch(message);
    }

}
