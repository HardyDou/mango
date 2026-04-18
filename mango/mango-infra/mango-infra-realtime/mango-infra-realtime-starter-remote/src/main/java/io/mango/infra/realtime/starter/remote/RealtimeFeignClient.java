package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.RealtimeMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign adapter for remote realtime publishing.
 */
@FeignClient(name = "mango-infra-realtime", path = "/internal/realtime")
public interface RealtimeFeignClient extends RealtimeApi {

    @Override
    @PostMapping("/publish")
    void publish(@RequestBody RealtimeMessage message);
}
