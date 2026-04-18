package io.mango.infra.realtime.starter;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal realtime publishing endpoint for remote starter calls.
 */
@RestController
@RequestMapping("/internal/realtime")
@RequiredArgsConstructor
@ConditionalOnBean(RealtimePublisher.class)
public class RealtimeApiController implements RealtimeApi {

    private final RealtimePublisher realtimePublisher;

    @Override
    @PostMapping("/publish")
    public void publish(@RequestBody RealtimeMessage message) {
        realtimePublisher.publish(message);
    }
}
