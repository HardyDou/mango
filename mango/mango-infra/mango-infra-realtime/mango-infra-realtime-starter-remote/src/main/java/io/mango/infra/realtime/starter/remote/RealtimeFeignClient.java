package io.mango.infra.realtime.starter.remote;

import io.mango.common.contract.NativeHttpAdapter;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign adapter for remote realtime publishing.
 */
@FeignClient(name = "mango-infra-realtime", contextId = "realtimeFeignClient", path = "/realtime/messages")
@NativeHttpAdapter
public interface RealtimeFeignClient {

    @PostMapping("/publish")
    void publish(@RequestBody RealtimeOutboundMessage realtimeOutboundMessage);
}
