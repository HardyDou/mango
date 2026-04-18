package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.RealtimeSubscriberApi;
import io.mango.infra.realtime.api.RealtimeSubscriberRegistration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mango-infra-realtime", path = "/internal/realtime/subscribers")
public interface RealtimeSubscriberFeignClient extends RealtimeSubscriberApi {

    @Override
    @PostMapping("/register")
    void register(@RequestBody RealtimeSubscriberRegistration registration);

    @Override
    @PostMapping("/unregister")
    void unregister(@RequestBody RealtimeSubscriberRegistration registration);
}
