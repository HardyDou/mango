package io.mango.infra.realtime.starter.remote;

import io.mango.common.contract.NativeHttpAdapter;
import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mango-infra-realtime", contextId = "realtimeInboundReceiverFeignClient", path = "/realtime/receivers")
@NativeHttpAdapter
public interface RealtimeInboundReceiverFeignClient {

    @PostMapping("/register")
    void register(@RequestBody RealtimeInboundReceiverRegistration registration);

    @PostMapping("/unregister")
    void unregister(@RequestBody RealtimeInboundReceiverRegistration registration);
}
