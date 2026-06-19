package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.RealtimeInboundReceiverApi;
import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mango-infra-realtime", contextId = "realtimeInboundReceiverFeignClient", path = "/realtime/receivers")
public interface RealtimeInboundReceiverFeignClient extends RealtimeInboundReceiverApi {

    @Override
    @PostMapping("/register")
    void register(@RequestBody RealtimeInboundReceiverRegistration registration);

    @Override
    @PostMapping("/unregister")
    void unregister(@RequestBody RealtimeInboundReceiverRegistration registration);
}
