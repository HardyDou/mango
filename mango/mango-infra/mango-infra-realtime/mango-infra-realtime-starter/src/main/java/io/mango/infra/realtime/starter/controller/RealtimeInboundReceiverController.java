package io.mango.infra.realtime.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.contract.NativeHttpAdapter;
import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;
import io.mango.infra.realtime.core.inbound.receiver.IRealtimeInboundReceiverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

/**
 * Receiver-service registry endpoint for forward remote calls.
 */
@Validated
@NativeHttpAdapter
@RestController
@RequestMapping("/realtime/receivers")
@RequiredArgsConstructor
@ApiAccess(mode = ApiResourceAccessMode.INTERNAL)
@Tag(name = "实时接收器", description = "实时消息接收器注册与注销接口")
public class RealtimeInboundReceiverController {

    private final IRealtimeInboundReceiverService realtimeInboundReceiverService;

    @PostMapping("/register")
    @Operation(summary = "注册实时接收器", description = "内部接口。注册可接收客户端上行实时消息的服务")
    public void register(@Valid @RequestBody RealtimeInboundReceiverRegistration registration) {
        realtimeInboundReceiverService.register(registration);
    }

    @PostMapping("/unregister")
    @Operation(summary = "注销实时接收器", description = "内部接口。注销实时上行消息接收服务")
    public void unregister(@Valid @RequestBody RealtimeInboundReceiverRegistration registration) {
        realtimeInboundReceiverService.unregister(registration);
    }
}
